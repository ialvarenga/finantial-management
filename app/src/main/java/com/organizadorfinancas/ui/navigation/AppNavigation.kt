import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.organizadorfinancas.ui.import.ImportStatementScreen
import java.net.URLDecoder
import java.net.URLEncoder

// Add this constant with your other route constants
const val IMPORT_STATEMENT_ROUTE = "import_statement/{creditCardId}/{creditCardName}"

// Add this composable inside your NavHost { ... }
        composable(
            route = IMPORT_STATEMENT_ROUTE,
            arguments = listOf(
                navArgument("creditCardId") { type = NavType.LongType },
                navArgument("creditCardName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val creditCardId = backStackEntry.arguments?.getLong("creditCardId") ?: 0L
            val creditCardName = URLDecoder.decode(
                backStackEntry.arguments?.getString("creditCardName") ?: "",
                "UTF-8"
            )

            ImportStatementScreen(
                creditCardId = creditCardId,
                creditCardName = creditCardName,
                onNavigateBack = { navController.popBackStack() }
            )
        }

// Add this extension function outside the NavHost
fun NavController.navigateToImportStatement(creditCardId: Long, creditCardName: String) {
    val encodedName = URLEncoder.encode(creditCardName, "UTF-8")
    navigate("import_statement/$creditCardId/$encodedName")
}
