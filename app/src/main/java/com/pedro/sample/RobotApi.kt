package com.pedro.sample

import android.annotation.SuppressLint
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

interface RobotApi {
    // 단말기 저장
    @FormUrlEncoded
    @POST("device-save")
    fun postDeviceSave(
        @Field("requestCode") requestCode: Int,
        @Field("id") id: String?,
        @Field("name") name: String?,
        @Field("dept") dept: String?,
        @Field("type") type: String?,
        @Field("group_id") group_id: String?,
        @Field("regid") regid: String?,
        @Field("mac") mac: String?,
        @Field("ip") ip: String?,
        @Field("mobile") mobile: String?,
        @Field("ostype") ostype: String?,
        @Field("osversion") osversion: String?,
        @Field("manufacturer") manufacturer: String?,
        @Field("model") model: String?,
        @Field("display") display: String?,
        @Field("extra1") extra1: String?,
        @Field("extra2") extra2: String?,
        @Field("extra3") extra3: String?,
        @Field("access") access: String?,
        @Field("permission") permission: String?
    ): Call<DeviceSaveResponse>
}

class RobotClient {

    companion object {
        private const val TAG = "RobotClient"

        private var instance: RobotApi? = null

        val api: RobotApi
            get() {
                return getInstance()
            }

        @Synchronized
        fun getInstance(): RobotApi {
            if (instance == null)
                instance = create()
            return instance as RobotApi
        }

        // 인스턴스 새로 생성
        fun reset() {
            instance = null
        }

        private fun create(): RobotApi {
            val httpLoggingInterceptor = HttpLoggingInterceptor()
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

            val clientBuilder = OkHttpClient.Builder()

            // SSL support START
            @SuppressLint("CustomX509TrustManager")
            val x509TrustManager: X509TrustManager = object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

                @Throws(CertificateException::class)
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                @Throws(CertificateException::class)
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                }
            }

            try {
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, SecureRandom())
                val sslSocketFactory = sslContext.socketFactory
                clientBuilder.sslSocketFactory(sslSocketFactory, x509TrustManager)
            } catch (e: Exception) {
            }

            clientBuilder.hostnameVerifier(RelaxedHostNameVerifier())
            // SSL support END

            val headerInterceptor = Interceptor {
                val request = it.request()
                    .newBuilder()
                    .build()
                return@Interceptor it.proceed(request)
            }
            clientBuilder.addInterceptor(headerInterceptor)
            clientBuilder.addInterceptor(httpLoggingInterceptor)
            clientBuilder.connectTimeout(10, TimeUnit.SECONDS)
            clientBuilder.readTimeout(10, TimeUnit.SECONDS)
            clientBuilder.writeTimeout(10, TimeUnit.SECONDS)

            val client = clientBuilder.build()

            return Retrofit.Builder()
                .baseUrl("https://119.6.3.91:40023/moms/v1/rcs/ltr/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RobotApi::class.java)
        }


        // SSL support START
        @SuppressLint("CustomX509TrustManager")
        private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

            @SuppressLint("TrustAllX509TrustManager")
            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
            }

            @SuppressLint("TrustAllX509TrustManager")
            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            }
        })

        class RelaxedHostNameVerifier : HostnameVerifier {
            @SuppressLint("BadHostnameVerifier")
            override fun verify(hostname: String, session: SSLSession): Boolean {
                return true
            }
        }
        // SSL support END
    }
}
