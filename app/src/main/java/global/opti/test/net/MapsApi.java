package global.opti.test.net;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;

public interface MapsApi {

    @Headers({
            "Content-Type:application/json"
    })
    @GET("user/change")
    Call<String> change(
            @retrofit2.http.Query("login") String login, @retrofit2.http.Query("md5") String md5
    );

}
