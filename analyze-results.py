import json
import sys
from datetime import datetime

def analyze_k6_results(json_file):
    """
    Analyse les résultats du test k6 et génère un rapport lisible
    """
    try:
        with open(json_file, 'r') as f:
            # Le fichier JSON de k6 contient une ligne par métrique
            lines = f.readlines()

        metrics = {}
        for line in lines:
            try:
                data = json.loads(line.strip())
                if data.get('type') == 'Point':
                    metric_name = data.get('metric')
                    if metric_name not in metrics:
                        metrics[metric_name] = []
                    metrics[metric_name].append(data)
            except json.JSONDecodeError:
                continue

        print("=" * 60)
        print("RAPPORT D'ANALYSE DU TEST DE CHARGE K6")
        print("=" * 60)
        print()

        # Analyse des requêtes HTTP
        if 'http_req_duration' in metrics:
            durations = [float(m['data']['value']) for m in metrics['http_req_duration']]
            print(f"📊 TEMPS DE RÉPONSE HTTP:")
            print(f"   • Moyenne: {sum(durations)/len(durations):.2f}ms")
            print(f"   • Minimum: {min(durations):.2f}ms")
            print(f"   • Maximum: {max(durations):.2f}ms")
            print(f"   • Total requêtes: {len(durations)}")
            print()

        # Analyse des erreurs
        if 'http_req_failed' in metrics:
            failures = [m['data']['value'] for m in metrics['http_req_failed']]
            total_requests = len(failures)
            failed_requests = sum(failures)
            success_rate = ((total_requests - failed_requests) / total_requests) * 100
            print(f"✅ TAUX DE SUCCÈS:")
            print(f"   • Succès: {success_rate:.1f}%")
            print(f"   • Échecs: {failed_requests}/{total_requests}")
            print()

        # Métriques personnalisées d'ordres
        if 'orders_created_total' in metrics:
            orders = metrics['orders_created_total']
            total_orders = len(orders)
            print(f"📈 ORDRES CRÉÉS:")
            print(f"   • Total d'ordres: {total_orders}")

            # Calcul du débit (ordres/seconde)
            if len(orders) > 1:
                first_time = min(m['data']['time'] for m in orders)
                last_time = max(m['data']['time'] for m in orders)
                duration_seconds = (last_time - first_time) / 1000000000  # Conversion nanosec -> sec
                throughput = total_orders / duration_seconds if duration_seconds > 0 else 0
                print(f"   • Débit moyen: {throughput:.1f} ordres/sec")
                print()

        # VUs (Virtual Users)
        if 'vus' in metrics:
            vus_data = [m['data']['value'] for m in metrics['vus']]
            print(f"👥 UTILISATEURS VIRTUELS:")
            print(f"   • Maximum atteint: {max(vus_data)} VUs")
            print(f"   • Moyenne: {sum(vus_data)/len(vus_data):.1f} VUs")
            print()

        print("=" * 60)
        print("RECOMMANDATIONS:")
        print("=" * 60)

        # Recommandations basées sur les résultats
        if 'http_req_duration' in metrics:
            avg_duration = sum(durations)/len(durations)
            if avg_duration > 2000:
                print("⚠️  Temps de réponse élevé (>2s) - optimisation nécessaire")
            elif avg_duration > 1000:
                print("🔶 Temps de réponse acceptable mais améliorable")
            else:
                print("✅ Excellents temps de réponse")

        if 'http_req_failed' in metrics and success_rate < 95:
            print("⚠️  Taux d'échec élevé - vérifiez les logs des services")

        if 'orders_created_total' in metrics and throughput < 800:
            print(f"🎯 Objectif de 800 ordres/s non atteint ({throughput:.1f})")
            print("   Considérez: scaling horizontal, optimisation DB, cache")
        elif 'orders_created_total' in metrics:
            print(f"🎉 Objectif de 800 ordres/s ATTEINT! ({throughput:.1f})")

        print()

    except FileNotFoundError:
        print(f"❌ Fichier {json_file} introuvable")
        print("Assurez-vous d'avoir exécuté le test k6 d'abord")
    except Exception as e:
        print(f"❌ Erreur lors de l'analyse: {e}")

if __name__ == "__main__":
    json_file = "test-results.json"
    if len(sys.argv) > 1:
        json_file = sys.argv[1]

    analyze_k6_results(json_file)
