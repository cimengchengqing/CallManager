package com.convenient.salescall.tools

import android.telephony.TelephonyManager
import com.convenient.salescall.app.CallApp
import com.convenient.salescall.datas.NettyMessage
import com.convenient.salescall.datas.UuidPrefs
import com.convenient.salescall.receiver.MessageCenter
import com.google.gson.Gson
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow

class NettyClient(private val host: String, private val port: Int) {
    private val group = NioEventLoopGroup()
    private var retryCount = 0
    private val maxRetryDelay = 30L
    private var isConnected = AtomicBoolean(false)
    private var currentChannel: Channel? = null
    private var bootstrap: Bootstrap? = null
    private val isConnecting = AtomicBoolean(false)
    private val isShutdown = AtomicBoolean(false)

    fun start() {
        if (isConnected.get() || isConnecting.get() || isShutdown.get()) return

        bootstrap = Bootstrap().apply {
            group(group)
            channel(NioSocketChannel::class.java)
            option(ChannelOption.SO_KEEPALIVE, true)
            option(ChannelOption.TCP_NODELAY, true)
            option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(
                        DelimiterBasedFrameDecoder(
                            8192, Unpooled.copiedBuffer("\n".toByteArray(StandardCharsets.UTF_8))
                        ),
                        StringDecoder(StandardCharsets.UTF_8),
                        StringEncoder(StandardCharsets.UTF_8),
                        ClientHandler(this@NettyClient)
                    )
                }
            })
        }
        connect()
    }

    private fun connect() {
        if (isConnecting.get() || isConnected.get() || isShutdown.get()) return
        if (!isConnecting.compareAndSet(false, true)) return

        currentChannel?.close()
        bootstrap?.connect(host, port)?.addListener { future ->
            isConnecting.set(false)
            if (future.isSuccess) {
                currentChannel = (future as ChannelFuture).channel()
                isConnected.set(true)
                retryCount = 0
                currentChannel?.closeFuture()?.addListener {
                    isConnected.set(false)
                    currentChannel = null
                    if (!isShutdown.get()) {
                        group.schedule({ connect() }, 3, TimeUnit.SECONDS)
                    }
                }
            } else {
                isConnected.set(false)
                retryCount++
                val delay = minOf(2.0.pow(retryCount).toLong(), maxRetryDelay)
                if (!isShutdown.get()) {
                    group.schedule({ connect() }, delay, TimeUnit.SECONDS)
                }
            }
        }
    }

    fun shutdown() {
        isShutdown.set(true)
        isConnected.set(false)
        isConnecting.set(false)
        currentChannel?.close()
        currentChannel = null
        group.shutdownGracefully()
    }

    fun isConnected(): Boolean = isConnected.get()

    fun sendHeartbeat() {
        if (isConnected()) {
            try {
                val telephonyManager = CallApp.appContext.getSystemService(android.content.Context.TELEPHONY_SERVICE) as TelephonyManager
                var phoneNumber = telephonyManager.line1Number ?: "无法获取号码"
                phoneNumber = phoneNumber.replace("+86", "")
                val msg = NettyMessage("HEARTBEAT", UuidPrefs.getUuid(CallApp.appContext)!!, phoneNumber)
                val heartbeatMsg = Gson().toJson(msg) + "\n"
                currentChannel?.writeAndFlush(heartbeatMsg)
                LogUtils.d("NettyClient", "💓 已发送心跳消息: $heartbeatMsg")
            } catch (e: Exception) {
                LogUtils.e("NettyClient", "发送心跳消息异常: ${e.message}")
            }
        }
    }

    inner class ClientHandler(private val client: NettyClient) : ChannelInboundHandlerAdapter() {
        override fun channelActive(ctx: ChannelHandlerContext) {
            try {
                val telephonyManager = CallApp.appContext.getSystemService(android.content.Context.TELEPHONY_SERVICE) as TelephonyManager
                var phoneNumber = telephonyManager.line1Number ?: "无法获取号码"
                phoneNumber = phoneNumber.replace("+86", "")
                val msg = NettyMessage("AUTH", UuidPrefs.getUuid(CallApp.appContext)!!, phoneNumber)
                val authMsg = Gson().toJson(msg) + "\n"
                ctx.writeAndFlush(authMsg)
                LogUtils.d("NettyClient", "已发送鉴权消息: $authMsg")
            } catch (e: Exception) {
                LogUtils.e("NettyClient", "发送鉴权消息异常: ${e.message}")
            }
            super.channelActive(ctx)
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            isConnected.set(false)
        }

        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            msg?.let {
                try {
                    val json = it as String
                    val nettyMessage = Gson().fromJson(json, NettyMessage::class.java)
                    if (nettyMessage != null && nettyMessage.data.isNotEmpty()) {
                        MessageCenter.post(nettyMessage.data)
                    }
                } catch (e: Exception) {
                    LogUtils.e("NettyClient", "JSON 解析失败: ${e.message}")
                }
            }
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            LogUtils.e("NettyClient", "连接异常: ${cause.message}")
            cause.printStackTrace()
            ctx.close()
        }
    }
}