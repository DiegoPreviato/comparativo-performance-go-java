package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"time"
)

func fibonacci(n int) int {
	if n <= 1 {
		return n
	}
	return fibonacci(n-1) + fibonacci(n-2)
}

type Request struct {
	Iterations int `json:"iterations"`
	N          int `json:"n"`
}

type Response struct {
	Result          int   `json:"result"`
	ExecutionTimeMs int64 `json:"execution_time_ms"`
}

func fibonacciHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Only POST method is allowed", http.StatusMethodNotAllowed)
		return
	}

	var req Request
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	start := time.Now()
	var result int
	for i := 0; i < req.Iterations; i++ {
		result = fibonacci(req.N)
	}
	duration := time.Since(start)

	resp := Response{
		Result:          result,
		ExecutionTimeMs: duration.Milliseconds(),
	}

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(resp); err != nil {
		http.Error(w, "Failed to encode response", http.StatusInternalServerError)
	}
}

func main() {
	// Serve OpenAPI and Swagger UI files from the same folder
	http.HandleFunc("/openapi.json", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "openapi.json")
	})
	http.HandleFunc("/swagger", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "swagger.html")
	})

	http.HandleFunc("/fibonacci", fibonacciHandler)
	fmt.Println("Go server listening on port 8080")
	if err := http.ListenAndServe(":8080", nil); err != nil {
		fmt.Printf("Error starting server: %s\n", err)
	}
}
