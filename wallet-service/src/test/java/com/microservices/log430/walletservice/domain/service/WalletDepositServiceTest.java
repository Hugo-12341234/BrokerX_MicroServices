package com.microservices.log430.walletservice.domain.service;

import com.microservices.log430.walletservice.domain.model.entities.Transaction;
import com.microservices.log430.walletservice.domain.model.entities.Wallet;
import com.microservices.log430.walletservice.domain.model.entities.WalletAudit;
import com.microservices.log430.walletservice.domain.port.in.WalletDepositPort;
import com.microservices.log430.walletservice.domain.port.out.PaymentServicePort;
import com.microservices.log430.walletservice.domain.port.out.TransactionPort;
import com.microservices.log430.walletservice.domain.port.out.WalletAuditPort;
import com.microservices.log430.walletservice.domain.port.out.WalletPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletDepositServiceTest {
    @Mock
    private TransactionPort transactionPort;
    @Mock
    private PaymentServicePort paymentServicePort;
    @Mock
    private WalletAuditPort walletAuditPort;
    @Mock
    private WalletPort walletPort;

    @InjectMocks
    private WalletDepositService walletDepositService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDeposit_Success() {
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("100.00");
        String idempotencyKey = "key-123";
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCreatedAt(LocalDateTime.now());
        wallet.setUpdatedAt(LocalDateTime.now());
        Transaction transaction = new Transaction(userId, idempotencyKey, amount, Transaction.TransactionType.DEPOSIT, "Dépôt au portefeuille");
        transaction.setId(10L);
        when(walletPort.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(transactionPort.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(transactionPort.save(any(Transaction.class))).thenReturn(transaction);
        when(paymentServicePort.processPayment(idempotencyKey, userId)).thenReturn(true);
        when(walletPort.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletDepositPort.DepositRequest request = new WalletDepositPort.DepositRequest(userId, amount, idempotencyKey);
        WalletDepositPort.DepositResult result = walletDepositService.deposit(request);

        assertTrue(result.isSuccess());
        assertEquals("Dépôt de 100.00$ effectué avec succès", result.getMessage());
        assertEquals(new BigDecimal("100.00"), result.getNewBalance());
        assertEquals(10L, result.getTransactionId());
        verify(walletPort).save(any(Wallet.class));
        verify(transactionPort, times(2)).save(any(Transaction.class));
        verify(walletAuditPort, atLeastOnce()).saveAudit(any(WalletAudit.class));
    }

    @Test
    void testDeposit_TooLowAmount() {
        Long userId = 2L;
        BigDecimal amount = new BigDecimal("5.00"); // < MIN_DEPOSIT
        String idempotencyKey = "key-low";
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.ZERO);
        when(walletPort.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(transactionPort.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(transactionPort.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        WalletDepositPort.DepositRequest request = new WalletDepositPort.DepositRequest(userId, amount, idempotencyKey);
        WalletDepositPort.DepositResult result = walletDepositService.deposit(request);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("minimum"));
        verify(transactionPort, times(2)).save(any(Transaction.class));
        verify(walletAuditPort).saveAudit(any(WalletAudit.class));
    }

    @Test
    void testDeposit_TooHighAmount() {
        Long userId = 3L;
        BigDecimal amount = new BigDecimal("60000.00"); // > MAX_DEPOSIT
        String idempotencyKey = "key-high";
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.ZERO);
        when(walletPort.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(transactionPort.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(transactionPort.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        WalletDepositPort.DepositRequest request = new WalletDepositPort.DepositRequest(userId, amount, idempotencyKey);
        WalletDepositPort.DepositResult result = walletDepositService.deposit(request);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("maximum"));
        verify(transactionPort, times(2)).save(any(Transaction.class));
        verify(walletAuditPort).saveAudit(any(WalletAudit.class));
    }

    @Test
    void testDeposit_PaymentRefused() {
        Long userId = 4L;
        BigDecimal amount = new BigDecimal("100.00");
        String idempotencyKey = "key-payfail";
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.ZERO);
        when(walletPort.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(transactionPort.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(transactionPort.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentServicePort.processPayment(idempotencyKey, userId)).thenReturn(false);
        WalletDepositPort.DepositRequest request = new WalletDepositPort.DepositRequest(userId, amount, idempotencyKey);
        WalletDepositPort.DepositResult result = walletDepositService.deposit(request);
        assertFalse(result.isSuccess());
        assertEquals("Paiement refusé", result.getMessage());
        verify(transactionPort, times(2)).save(any(Transaction.class));
        verify(walletAuditPort).saveAudit(any(WalletAudit.class));
    }

    @Test
    void testDeposit_IdempotentSettled() {
        Long userId = 5L;
        BigDecimal amount = new BigDecimal("100.00");
        String idempotencyKey = "key-idem-settled";
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(new BigDecimal("200.00"));
        Transaction transaction = new Transaction(userId, idempotencyKey, amount, Transaction.TransactionType.DEPOSIT, "Dépôt au portefeuille");
        transaction.setId(99L);
        transaction.markAsSettled();
        when(walletPort.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(transactionPort.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(transaction));
        WalletDepositPort.DepositRequest request = new WalletDepositPort.DepositRequest(userId, amount, idempotencyKey);
        WalletDepositPort.DepositResult result = walletDepositService.deposit(request);
        assertTrue(result.isSuccess());
        assertEquals("Dépôt déjà effectué", result.getMessage());
        assertEquals(new BigDecimal("200.00"), result.getNewBalance());
        assertEquals(99L, result.getTransactionId());
    }

    @Test
    void testDeposit_IdempotentPending() {
        Long userId = 6L;
        BigDecimal amount = new BigDecimal("100.00");
        String idempotencyKey = "key-idem-pending";
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(new BigDecimal("300.00"));
        Transaction transaction = new Transaction(userId, idempotencyKey, amount, Transaction.TransactionType.DEPOSIT, "Dépôt au portefeuille");
        transaction.setId(101L);
        // Status par défaut: PENDING
        when(walletPort.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(transactionPort.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(transaction));
        WalletDepositPort.DepositRequest request = new WalletDepositPort.DepositRequest(userId, amount, idempotencyKey);
        WalletDepositPort.DepositResult result = walletDepositService.deposit(request);
        assertFalse(result.isSuccess());
        assertEquals("Transaction en cours de traitement", result.getMessage());
        assertNull(result.getTransactionId());
    }

    @Test
    void testDeposit_CreateWalletIfNotExists() {
        Long userId = 7L;
        BigDecimal amount = new BigDecimal("100.00");
        String idempotencyKey = "key-create-wallet";
        when(walletPort.findByUserId(userId)).thenReturn(Optional.empty());
        Wallet newWallet = new Wallet();
        newWallet.setUserId(userId);
        newWallet.setBalance(BigDecimal.ZERO);
        newWallet.setCreatedAt(LocalDateTime.now());
        newWallet.setUpdatedAt(LocalDateTime.now());
        when(walletPort.save(any(Wallet.class))).thenReturn(newWallet);
        when(transactionPort.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        Transaction transaction = new Transaction(userId, idempotencyKey, amount, Transaction.TransactionType.DEPOSIT, "Dépôt au portefeuille");
        transaction.setId(111L);
        when(transactionPort.save(any(Transaction.class))).thenReturn(transaction);
        when(paymentServicePort.processPayment(idempotencyKey, userId)).thenReturn(true);
        WalletDepositPort.DepositRequest request = new WalletDepositPort.DepositRequest(userId, amount, idempotencyKey);
        WalletDepositPort.DepositResult result = walletDepositService.deposit(request);
        assertTrue(result.isSuccess());
        assertEquals("Dépôt de 100.00$ effectué avec succès", result.getMessage());
        verify(walletPort, times(2)).save(any(Wallet.class)); // création + update solde
        verify(walletAuditPort, atLeastOnce()).saveAudit(any(WalletAudit.class));
    }

    @Test
    void testDeposit_TechnicalException() {
        Long userId = 8L;
        BigDecimal amount = new BigDecimal("100.00");
        String idempotencyKey = "key-exception";
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.ZERO);
        when(walletPort.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(transactionPort.findByIdempotencyKey(idempotencyKey)).thenThrow(new RuntimeException("DB error"));
        WalletDepositPort.DepositRequest request = new WalletDepositPort.DepositRequest(userId, amount, idempotencyKey);
        WalletDepositPort.DepositResult result = walletDepositService.deposit(request);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Erreur technique"));
        verify(walletAuditPort).saveAudit(any(WalletAudit.class));
    }
}
