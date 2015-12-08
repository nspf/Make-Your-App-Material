/*
 * Copyright 2015 Nicolas Pintos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.xyzreader.remote;

import retrofit.Retrofit;
import retrofit.GsonConverterFactory;

public class RestClient {

    private static final String BASE_URL = "http://dl.dropboxusercontent.com/u/231329/xyzreader_data/";
    private final RestService mRestClient;


    private RestClient() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mRestClient = retrofit.create(RestService.class);
    }

    private RestService getRestClient() {
        return mRestClient;
    }


    public static RestService build() {
        return new RestClient().getRestClient();

    }
}