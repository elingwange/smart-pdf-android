import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.ui.components.SearchItem
import com.quantumstudio.smartpdf.ui.components.SearchTopBar
import com.quantumstudio.smartpdf.ui.features.main.MainViewModel
import com.quantumstudio.smartpdf.ui.features.search.SearchEmptyState


@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onFileClick: (PdfFile) -> Unit
) {
    // 1. 观察来自 ViewModel 的状态
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResult.collectAsState()

    // 2. 焦点控制器：用于进入页面后自动弹出键盘
    val focusRequester = remember { FocusRequester() }

    // 3. 生命周期处理：页面挂载后立即请求焦点
    LaunchedEffect(Unit) {
        // 稍微延迟 100ms 确保 Compose 渲染树准备就绪，提高弹出成功率
        kotlinx.coroutines.delay(100)
        focusRequester.requestFocus()
    }

    // 4. 使用 Surface 包裹以提供不透明背景，实现全屏遮罩效果
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 5. 顶栏搜索框
            SearchTopBar(
                query = query,
                onQueryChange = { viewModel.onQueryChange(it) },
                onBack = {
                    // 返回前清空搜索状态，确保下次进来是干净的
                    viewModel.onQueryChange("")
                    onBack()
                },
                focusRequester = focusRequester
            )

            // 6. 搜索内容区
            Box(modifier = Modifier.weight(1f)) {
                when {
                    // 情况 A: 搜索框不为空且找不到结果 -> 显示插画
                    query.isNotEmpty() && results.isEmpty() -> {
                        SearchEmptyState()
                    }

                    // 情况 B: 搜索框为空 -> 可以留白或显示“搜索提示”
                    query.isEmpty() -> {
                        // 如果需要显示“最近搜索”，可以放在这里
                    }

                    // 情况 C: 搜索到结果 -> 显示列表
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(
                                items = results,
                                key = { it.path } // 使用路径作为唯一 Key 提升性能
                            ) { pdf ->
                                SearchItem(
                                    pdf = pdf,
                                    query = query,
                                    onClick = { onFileClick(pdf) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}