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
import ru.bingosoft.teploInspector.util.OtherUtil
import ru.bingosoft.teploInspector.util.SharedPrefSaver
import timber.log.Timber
import java.util.concurrent.TimeUnit


@Module
class NetworkModule {

    @Provides
    fun providesApiService(sharedPrefSaver: SharedPrefSaver) : ApiService {

        /*val interceptor = if (BuildConfig.DEBUG) {
            Timber.d("DEBUG")
             HttpLoggingInterceptor()
        } else {
            Timber.d("RELEASE")
            val fileLogger=object:HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    writeToFile(message)
                }

            }
            HttpLoggingInterceptor(fileLogger)
        }
        interceptor.level= HttpLoggingInterceptor.Level.BODY*/

        val fileLogger=object:HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                OtherUtil().writeToFile(message)
            }

        }
        val interceptorFile=HttpLoggingInterceptor(fileLogger)
        interceptorFile.level=HttpLoggingInterceptor.Level.BODY

        val interceptor=HttpLoggingInterceptor()
        interceptor.level= HttpLoggingInterceptor.Level.BODY


        val client = OkHttpClient.Builder()
            .addInterceptor(interceptorFile)
            .addInterceptor(interceptor)
            .addInterceptor(object : Interceptor {
                override fun intercept(chain: Interceptor.Chain): Response {
                    var token = ""
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
            .connectTimeout(60, TimeUnit.SECONDS) // Увеличим таймаут ретрофита
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
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

}