package ru.bingosoft.mapquestapp2.di

import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import ru.bingosoft.mapquestapp2.BuildConfig
import ru.bingosoft.mapquestapp2.api.ApiService

@Module
class NetworkModule {
    @Provides
    fun providesApiService() : ApiService {

        val interceptor = HttpLoggingInterceptor()
        interceptor.level= HttpLoggingInterceptor.Level.BODY
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd")
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