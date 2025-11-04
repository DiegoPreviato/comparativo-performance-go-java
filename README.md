# comparativo-performance-go-java

Este repositório contém duas implementações de um serviço simples que calcula o n-ésimo número de Fibonacci um número de vezes (para testes de performance):

- `go/` - servidor HTTP Go ouvindo na porta `8080`.
- `java/` - servidor HTTP Java (usando `com.sun.net.httpserver`) ouvindo na porta `8081`.

Cada implementação expõe um endpoint POST `/fibonacci` que recebe JSON com os campos `iterations` e `n` e retorna `{ "result": <fib>, "execution_time_ms": <ms> }`.

Resumo do comparativo (medição fornecida pelo autor):

- Java (100 iterações): "execution_time_ms": 31714
- Go   (100 iterações): "execution_time_ms": 46329

Observação: nos testes do autor o Java apresentou melhor desempenho nesse cenário específico (recursão pura de Fibonacci). Esses números dependem fortemente de JIT, GC, otimizações do runtime e do valor de `n` usado. Use estes resultados como indicação para este caso particular, não como regra geral.

Endpoints úteis

- POST /fibonacci - body: `{ "iterations": 100, "n": 30 }`
- GET  /openapi.json - OpenAPI 3 definition for the API
- GET  /swagger - Small Swagger UI to test the API (carrega `/openapi.json`)

Como rodar

Go (recomendado executar dentro da pasta `go`):

```powershell
cd go
go run main.go
# Abrir http://localhost:8080/swagger no navegador
```

Java (recomendado executar dentro da pasta `java`):

```powershell
cd java
# Compilar
javac Main.java
# Executar
java Main
# Abrir http://localhost:8081/swagger no navegador
```

Testar via curl

```powershell
curl -s -X POST http://localhost:8080/fibonacci -H "Content-Type: application/json" -d '{"iterations":100,"n":30}' | jq

curl -s -X POST http://localhost:8081/fibonacci -H "Content-Type: application/json" -d '{"iterations":100,"n":30}' | jq
```

Notas e próximos passos

- Os endpoints `/swagger` usam Swagger UI via CDN e carregam `/openapi.json` servido pelo mesmo servidor. Se for necessário servir os assets localmente, posso adicionar os arquivos do Swagger UI.
- Se quiser replicar os testes e coletar métricas mais controladas (várias repetições, valores distintos de `n`, profiler), posso adicionar scripts de benchmark e instruções.
