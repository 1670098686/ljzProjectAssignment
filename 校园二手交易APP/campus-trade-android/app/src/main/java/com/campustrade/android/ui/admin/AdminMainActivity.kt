package com.campustrade.android.ui.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campustrade.android.ui.theme.CampusTradeTheme
import com.campustrade.android.util.SharedPreferencesHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AdminMainActivity : ComponentActivity() {
    
    @Inject
    lateinit var sharedPreferencesHelper: SharedPreferencesHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            CampusTradeTheme {
                AdminMainScreen(
                    onLogout = { finish() },
                    adminInfo = sharedPreferencesHelper.getAdminInfo()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    onLogout: () -> Unit,
    adminInfo: com.campustrade.android.model.User?
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "管理员控制台",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("退出登录")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 管理员信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "管理员信息",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    adminInfo?.let { admin ->
                        Text("用户名：${admin.username}")
                        Text("邮箱：${admin.email}")
                        Text("角色：管理员")
                    } ?: Text("未获取到管理员信息")
                }
            }
            
            // 管理功能卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "管理功能",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    // 用户管理按钮
                    Button(
                        onClick = { /* TODO: 跳转到用户管理页面 */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("用户管理")
                    }
                    
                    // 商品管理按钮
                    Button(
                        onClick = { /* TODO: 跳转到商品管理页面 */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("商品管理")
                    }
                    
                    // 订单管理按钮
                    Button(
                        onClick = { /* TODO: 跳转到订单管理页面 */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("订单管理")
                    }
                    
                    // 系统设置按钮
                    Button(
                        onClick = { /* TODO: 跳转到系统设置页面 */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("系统设置")
                    }
                }
            }
            
            // 统计信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "系统统计",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("总用户数", fontSize = 12.sp)
                            Text("0", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("总商品数", fontSize = 12.sp)
                            Text("0", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("总订单数", fontSize = 12.sp)
                            Text("0", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}