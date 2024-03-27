package com.serafimtech.serafimaudio.Compose

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.serafimtech.serafimaudio.R
import com.serafimtech.serafimaudio.ViewModel
import kotlinx.coroutines.launch

@Composable
fun QuestionnaireScreen(model: ViewModel) {
    val context = LocalContext.current
    var rememberWebProgress: Int by remember { mutableStateOf(-1) }
    val Address by model.AddressLiveData.observeAsState()

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.weight(1f)) {
            CustomWebView(
                modifier = Modifier.fillMaxSize(),
                url = context.resources.getString(R.string.Warranty),
                onProgressChange = { progress ->
                    rememberWebProgress = progress
                },
                initSettings = { settings ->
                    settings?.apply {
                        //支持js交互
                        javaScriptEnabled = true
                        //....
                    }
                },
                onBack = { webView ->
                    //可根据需求处理此处
                    if (webView?.canGoBack() == true) {
                        //返回上一级页面
                        webView.goBack()
                    } else {
                        //关闭activity
//                finish()
                    }
                },
                onReceivedError = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                Log.d(TAG,">>>>>>${it?.description}")
                    }
                },
                Address = Address!!,
                model = model
            )

            LinearProgressIndicator(
                progress = rememberWebProgress * 1.0F / 100F,
                color = Color.Red,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (rememberWebProgress == 100) 0.dp else 5.dp)
            )
        }

        TextButton(
            onClick = { model.setPageData(Navigator.NavTarget.Main.label) },
        ) {
            Text(context.resources.getString(R.string.questionnaire_skip), color = Color.Gray)
        }
    }
}

@Composable
fun CustomWebView(
    modifier: Modifier = Modifier,
    url: String,
    Address: String,
    onBack: (webView: WebView?) -> Unit,
    onProgressChange: (progress: Int) -> Unit = {},
    initSettings: (webSettings: WebSettings?) -> Unit = {},
    onReceivedError: (error: WebResourceError?) -> Unit = {},
    model: ViewModel,

    ) {
    val context = LocalContext.current
    val js =
        "javascript:document.getElementById('" + context.resources.getString(R.string.DeviceIDinput) + "').value='" + Address + "';"
    val webViewChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            //回调网页内容加载进度
            onProgressChange(newProgress)
            super.onProgressChanged(view, newProgress)
        }
    }
    val webViewClient = object : WebViewClient() {
        override fun onPageStarted(
            view: WebView?, url: String?,
            favicon: Bitmap?,
        ) {
            super.onPageStarted(view, url, favicon)
            onProgressChange(-1)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)

            if (Build.VERSION.SDK_INT >= 19) {
                view!!.evaluateJavascript(js) { }
            } else {
                view!!.loadUrl(js)
            }

            onProgressChange(100)
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?,
        ): Boolean {
            if (null == request?.url) return false
            val showOverrideUrl = request.url.toString()

            //TODO 改成線上讀取字串
            if (request.url.toString().contains("home")) {
                model.setPageData(Navigator.NavTarget.Main.label)
                return true
            }
            try {
                if (!showOverrideUrl.startsWith("http://")
                    && !showOverrideUrl.startsWith("https://")
                ) {
                    //处理非http和https开头的链接地址
                    Intent(Intent.ACTION_VIEW, Uri.parse(showOverrideUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        view?.context?.applicationContext?.startActivity(this)
                    }
                    return true
                }
            } catch (e: Exception) {
                //没有安装和找到能打开(「xxxx://openlink.cc....」、「weixin://xxxxx」等)协议的应用
                return true
            }
            return super.shouldOverrideUrlLoading(view, request)
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?,
        ) {
            super.onReceivedError(view, request, error)
            //自行处理....
            onReceivedError(error)
        }
    }
    var webView: WebView? = null
    val coroutineScope = rememberCoroutineScope()
    AndroidView(modifier = modifier, factory = { ctx ->
        WebView(ctx).apply {
            this.webViewClient = webViewClient
            this.webChromeClient = webViewChromeClient
            //回调webSettings供调用方设置webSettings的相关配置


            initSettings(this.settings)
            webView = this
            loadUrl(url)
        }
    })
    BackHandler {
        coroutineScope.launch {
            //自行控制点击了返回按键之后，关闭页面还是返回上一级网页
            onBack(webView)
        }
    }
}