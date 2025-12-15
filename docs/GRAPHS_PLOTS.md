# Guia de Melhorias - Organizador de Finanças

## Gráficos e Análises

### Objetivo
Visualizar gastos por categoria, histórico mensal, comparativos e tendências.

### Biblioteca Recomendada
**Vico** (vico-compose) - Biblioteca nativa para Compose, leve e customizável.

```kotlin
implementation("com.patrykandpatrick.vico:compose-m3:1.13.1")
```

Alternativas: MPAndroidChart (mais completa, mas não é Compose-native), YCharts

### Gráficos Planejados

| Tipo | Uso | Descrição |
|------|-----|-----------|
| Pizza/Donut | Gastos por Categoria | Distribuição do mês atual, cores por categoria |
| Barras | Histórico Mensal | Últimos 6-12 meses, comparação mês a mês |
| Linha | Evolução de Gastos | Tendência ao longo do tempo, média móvel |
| Barras Empilhadas | Receitas vs Despesas | Fluxo de caixa por mês |
| Barras Horizontais | Top Categorias | Ranking dos maiores gastos |

### Nova Tela: `AnalyticsScreen`

```
AnalyticsScreen
├── PeriodSelector (Mês atual, 3 meses, 6 meses, 1 ano)
├── SummaryCards (Total gasto, média, maior gasto)
├── TabRow
│   ├── Tab: Categorias → PieChart
│   ├── Tab: Histórico → BarChart
│   └── Tab: Tendências → LineChart
└── DetailsList (breakdown clicável)
```

### Queries Necessárias

```kotlin
// Gastos por categoria
@Query("""
    SELECT category, SUM(amount) as total 
    FROM credit_card_items 
    WHERE purchaseDate >= :startDate AND purchaseDate < :endDate
    GROUP BY category ORDER BY total DESC
""")
fun getSpendingByCategory(startDate: Long, endDate: Long): Flow<List<CategoryTotal>>

// Histórico mensal
@Query("""
    SELECT strftime('%Y-%m', purchaseDate/1000, 'unixepoch') as month, SUM(amount) as total
    FROM credit_card_items WHERE purchaseDate >= :startDate
    GROUP BY month ORDER BY month ASC
""")
fun getMonthlySpending(startDate: Long): Flow<List<MonthlyTotal>>

// Top estabelecimentos
@Query("""
    SELECT description, SUM(amount) as total, COUNT(*) as count
    FROM credit_card_items WHERE purchaseDate >= :startDate AND purchaseDate < :endDate
    GROUP BY description ORDER BY total DESC LIMIT :limit
""")
fun getTopMerchants(startDate: Long, endDate: Long, limit: Int = 10): Flow<List<MerchantTotal>>
```

### Data Classes

```kotlin
data class CategoryTotal(val category: String, val total: Double)
data class MonthlyTotal(val month: String, val total: Double)
data class MerchantTotal(val description: String, val total: Double, val count: Int)
```

### Tarefas

- [ ] Adicionar dependência do Vico
- [ ] Criar queries de agregação no DAO
- [ ] Criar `AnalyticsViewModel`
- [ ] Implementar `PieChartCard` (gastos por categoria)
- [ ] Implementar `BarChartCard` (histórico mensal)
- [ ] Implementar `LineChartCard` (tendências)
- [ ] Criar `AnalyticsScreen` com tabs
- [ ] Adicionar seletor de período
- [ ] Adicionar na navegação (nova aba "Análises")

### Localização na UI

**Recomendado:** Nova aba no BottomNavigation → "Análises" com ícone `Icons.Default.Analytics`

Opcional: Mini-gráfico na HomeScreen com "Ver mais" que leva para AnalyticsScreen