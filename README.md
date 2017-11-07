## Merchant Proxy

The merchant proxy is a wrapper server around the java SDK, that enables
merchants to accept and initiate payments from a user, using a simple HTTP API.

Once a tokenID is obtained (from the website), one can simply call GET /tokens/:id to 
get the details of that token, or PPST /transfers to make a transfer on that token.

The steps to achieve this are the following:
1. Use the merchant-sample-js or merchant-sample-java to create a website with a Token
button, or create a custom website with the button, and the client side javascript code
explained in the[merchant checkout docs](https://developer.token.io/merchant-checkout/).
2. Change the application.conf file, with email and bank account number to receive funds
3. Start this proxy server locally (./gradlew build run)
4. Call GET /member to get the alias of the member
5. Make sure the alias is the same one that is configured in the client side code (and lowercase)
6. A user now checks out using the button, and a tokenId is received after the user approves
7he transaction. (This will require a download of the Token app).
8. Call GET /tokens/:id to get details on that token
9. Call POST /transfers to initiate a transfer using this token. This requires some data (amount, currency), which is explained below. 


### Get member -  GET /member
```bash
curl -X GET "http://127.0.0.1:4567/member"
```

```json
{
  "memberId": "m:m2hkupUjtJRFKvpfD3vBKvTXM4J:5zKtXEAq",
  "aliases": [{
    "type": "EMAIL",
    "value": "merchanta@+noverify@example.com"
  }]
}
```

###  Get Token - GET /tokens/:id
```bash
curl -X GET "http://127.0.0.1:4567/tokens/tt:3A38Qpt8bKUsoBWaLi6bFCskkhHrVZyrVhyNDkFXYUdw:2gFuVeuQjGm"
```

```json
{
  "token": {
    "id": "tt:3A38Qpt8bKUsoBWaLi6bFCskkhHrVZyrVhyNDkFXYUdw:2gFuVeuQjGm",
    "payload": {
      "version": "1.0",
      "refId": "5wo7346idujau1h565hhtp",
      "issuer": {
        "alias": {
          "type": "EMAIL",
          "value": "silver@token.io"
        }
      },
      "from": {
        "id": "m:3mYtK26VeJ9MD3AvK2ou1X9vnNe9:5zKtXEAq"
      },
      "expiresAtMs": "1510105727024",
      "transfer": {
        "redeemer": {
          "id": "m:m2hkupUjtJRFKvpfD3vBKvTXM4J:5zKtXEAq",
          "alias": {
            "type": "EMAIL",
            "value": "merchanta@+noverify@example.com"
          }
        },
        "instructions": {
          "source": {
            "account": {
              "token": {
                "memberId": "m:3mYtK26VeJ9MD3AvK2ou1X9vnNe9:5zKtXEAq",
                "accountId": "a:JBU4w5EE31s61aiff1j9S8ANcH7svvotq13F37mAjrz:2gFuVeuQjGm"
              }
            }
          }
        },
        "currency": "EUR",
        "lifetimeAmount": "4.99",
        "pricing": {
          "sourceQuote": {
            "id": "b3e173646208464e8bd9d2fea1b36589",
            "accountCurrency": "EUR",
            "feesTotal": "0.25",
            "fees": [{
              "amount": "0.17",
              "description": "Transaction Fee"
            }, {
              "amount": "0.08",
              "description": "Initiation Fee"
            }],
            "expiresAtMs": "1510105727024"
          },
          "instructions": {
            "feesPaidBy": "SHARED_FEE",
            "fxPerformedBy": "SHARED_FX"
          }
        }
      }
    },
    "payloadSignatures": [{
      "action": "ENDORSED",
      "signature": {
        "memberId": "m:2B6MCv4tsBcFAgidgd3jwkyibYuB:5zKtXEAq",
        "keyId": "CqSTHPvWY_dgVh-f",
        "signature": "nQVCVmIL6qJ3dP63PjsWjF67g7RxJU457lpUuxlgHPpFCOlYlain0WC6fop-Lm_hXKAj-bRNyiCS-Mob6ERpBQ"
      }
    }, {
      "action": "ENDORSED",
      "signature": {
        "memberId": "m:3mYtK26VeJ9MD3AvK2ou1X9vnNe9:5zKtXEAq",
        "keyId": "sbKydDVML5zbUylC",
        "signature": "QKOaWFnMbODhKWgFctos0VPE-DSj9-egsJWutDpBA1KQjuVJLAeoOzNAAEcXGlNf9pTnSkRCcdC53OqT8fK1AA"
      }
    }]
  }
}
```

### Create Transfer - POST /transfers
```bash
curl -X POST -H 'Content-Type: application/json' "http://127.0.0.1:4567/transfers" -d '{amount:2, currency:"EUR", tokenId:"tt:3A38Qpt8bKUsoBWaLi6bFCskkhHrVZyrVhyNDkFXYUdw:2gFuVeuQjGm"}'
```

```json
{
  "transfer": {
    "id": "t:AX1T6DAoTDKyfuksjx7TrF3txXhEvDkZGYGo4VNBdfNm:2gFuVeuQjGm",
    "transactionId": "2e5333900cbc4f70a853b743df33c5c7",
    "createdAtMs": "1510019401079",
    "payload": {
      "refId": "nocPnqcKYnXBLHTXtAec",
      "tokenId": "tt:3A38Qpt8bKUsoBWaLi6bFCskkhHrVZyrVhyNDkFXYUdw:2gFuVeuQjGm",
      "amount": {
        "currency": "EUR",
        "value": "2.0"
      },
      "destinations": [{
        "account": {
          "sepa": {
            "iban": "IBAN123456789"
          }
        }
      }]
    },
    "payloadSignatures": [{
      "memberId": "m:2B6MCv4tsBcFAgidgd3jwkyibYuB:5zKtXEAq",
      "keyId": "CqSTHPvWY_dgVh-f",
      "signature": "UtuG0dcmI0fi4sJf59WohxPri-9PvppMkbh9GDqah4AU2BnLr_OAu8Z14Qjb97yqPRTmI-EIJ5BGYyM9N-GPAA"
    }, {
      "memberId": "m:m2hkupUjtJRFKvpfD3vBKvTXM4J:5zKtXEAq",
      "keyId": "_URoyRxmID8eKYv-",
      "signature": "XgBCXbVpzeVPiHLtVA1-QEDxktbLcP7PI-hduq8bpaM_tmA8eg1s8g4Z0rsLZn9Uyvq3Wfs3MN_LR3C8wc_EAg"
    }],
    "status": "SUCCESS"
  }
}
```

