package ru.bingosoft.teploInspector.di

import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import ru.bingosoft.teploInspector.BuildConfig
import ru.bingosoft.teploInspector.api.ApiService
import ru.bingosoft.teploInspector.util.Const.Network.TIMEOUT
import ru.bingosoft.teploInspector.util.OtherUtil
import ru.bingosoft.teploInspector.util.SharedPrefSaver
import timber.log.Timber
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


@Module
class NetworkModule {

    @Provides
    fun providesApiService(sharedPrefSaver: SharedPrefSaver, otherUtil: OtherUtil) : ApiService {

        val fileLogger=object:HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                otherUtil.writeToFile(message)
            }

        }
        val interceptorFile=HttpLoggingInterceptor(fileLogger)
        interceptorFile.level=HttpLoggingInterceptor.Level.BODY

        val interceptor=HttpLoggingInterceptor()
        interceptor.level= HttpLoggingInterceptor.Level.BODY


        val client = OkHttpClient.Builder()
        //val client = getUnsafeOkHttpClient()
            .addInterceptor(interceptorFile)
            .addInterceptor(interceptor)
            .addInterceptor(object : Interceptor {
                override fun intercept(chain: Interceptor.Chain): Response {
                    val token: String
                    if (sharedPrefSaver.sptoken.isEmpty()) {
                        token = sharedPrefSaver.getToken()
                        sharedPrefSaver.sptoken = token
                    } else {
                        token = sharedPrefSaver.sptoken
                    }


                    Timber.d("token=$token")
                    val newRequest = chain.request().newBuilder()
                        //.addHeader("Content-Type","application/json")
                        .addHeader("Authorization", token)
                        .build()

                    Timber.d("newRequest=$newRequest")

                    val response = chain.proceed(newRequest)
                    if (response.code == 200) {

                        val newToken = response.header("X-Auth-Token", "")
                        if (!newToken.isNullOrEmpty()) {
                            Timber.d("Обновили_токен")
                            sharedPrefSaver.sptoken = newToken
                            sharedPrefSaver.saveToken(newToken)
                        }

                    }

                    return response
                }
            })
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS) // Увеличим таймаут ретрофита секунд
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd H:mm")
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(BuildConfig.urlServer)
            .client(client)
            .build()

        return retrofit.create(ApiService::class.java)

    }

    //#Отключение_SSL
    //Использовать в крайнем случае - дыра в безопасности
    private fun getUnsafeOkHttpClient(): OkHttpClient.Builder {
        try {
            // Создаем менеджера доверия, который не проверяет цепочки сертификатов
            val trustAllCerts: Array<TrustManager> = arrayOf<TrustManager>(
                object : X509TrustManager {
                    @Throws(CertificateException::class)
                    override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {
                    }

                    @Throws(CertificateException::class)
                    override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return arrayOf()
                    }

                }
            )

            // Устанавливаем полностью доверительный менеджер
            val sslContext: SSLContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            // Создаем фабрику с полностью доверительным менеджером
            val sslSocketFactory: SSLSocketFactory = sslContext.getSocketFactory()
            val builder = OkHttpClient.Builder()
            builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier{ _, _ -> true }
            return builder
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

}