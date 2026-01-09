# Payment Service - Stripe Integration

## Opis
Serwis obsługujący płatności przez Stripe API. Wykorzystuje Stripe Checkout do bezpiecznego przetwarzania płatności za zamówienia pizzy.

## Funkcjonalności
- Tworzenie sesji Stripe Checkout
- Obsługa powrotów po udanej płatności
- Obsługa anulowanych płatności
- Przechowywanie statusów płatności w bazie danych
- Komunikacja z order-service w celu aktualizacji statusu zamówienia

## Konfiguracja

### 1. Uzyskaj klucze API ze Stripe
1. Załóż konto na https://stripe.com
2. Przejdź do Dashboard: https://dashboard.stripe.com/test/apikeys
3. Skopiuj **Secret Key** (zaczyna się od `sk_test_`)

### 2. Skonfiguruj zmienne środowiskowe

#### Opcja A: Plik .env (ZALECANA dla lokalnego developmentu)
1. Skopiuj plik `.env.example`:
   ```bash
   cp .env.example .env
   ```
2. Edytuj `.env` i wpisz swój klucz Stripe:
   ```bash
   STRIPE_SECRET_KEY=sk_test_twoj_klucz_testowy_z_stripe
   ```
3. **UWAGA**: Plik `.env` jest w `.gitignore` - nie zostanie commitowany do repozytorium!

#### Opcja B: IntelliJ IDEA - Environment Variables
1. Otwórz **Run → Edit Configurations**
2. Wybierz konfigurację `PaymentServiceApplication`
3. W sekcji **Environment Variables** dodaj:
   ```
   STRIPE_SECRET_KEY=sk_test_twoj_klucz_testowy_z_stripe
   ```
4. Kliknij **Apply** i **OK**

#### Opcja C: Windows PowerShell (tymczasowo dla sesji)
```powershell
$env:STRIPE_SECRET_KEY="sk_test_twoj_klucz_testowy_z_stripe"
mvn spring-boot:run
```

### 3. Uruchom serwis
```bash
# Lokalnie z Maven
mvn spring-boot:run

# Lub z Docker Compose (w głównym katalogu projektu)
docker-compose up payment-service
```

## Bezpieczeństwo
⚠️ **NIGDY NIE COMMITUJ KLUCZY API DO REPOZYTORIUM!**
- Wszystkie klucze trzymaj w zmiennych środowiskowych
- Plik `.env` jest w `.gitignore`
- Każdy członek zespołu ma swój własny klucz testowy Stripe

## Endpointy API

### POST /api/payments/create-checkout-session
Tworzy sesję Stripe Checkout
```json
{
  "orderId": 1,
  "amount": 45.99,
  "currency": "pln"
}
```

### POST /api/payments/success?session_id={sessionId}
Obsługuje powrót po udanej płatności

### POST /api/payments/cancel?session_id={sessionId}
Obsługuje anulowanie płatności

### GET /api/payments/order/{orderId}
Pobiera płatność dla zamówienia

## Baza danych
- PostgreSQL
- Tabela: `payments`
- Port: 5437 (lokalnie)

## Integracja z innymi serwisami
- **order-service**: Aktualizacja statusu zamówienia po płatności
- **gateway-service**: Routing przez API Gateway
- **config-server**: Centralna konfiguracja
- **discovery-server**: Service discovery przez Eureka

