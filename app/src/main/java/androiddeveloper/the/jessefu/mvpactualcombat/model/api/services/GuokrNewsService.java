package androiddeveloper.the.jessefu.mvpactualcombat.model.api.services;

import androiddeveloper.the.jessefu.mvpactualcombat.model.guokrNews.GuokrNewsBean;
import retrofit2.http.GET;
import retrofit2.http.Query;


/**
 * Created by Jesse Fu on 2017-04-30.
 * 获取果壳精选文章列表
 */

public interface GuokrNewsService {

    @GET("handpick/article.json")
    io.reactivex.Observable<GuokrNewsBean> getGuokrNews(@Query("retrieve_type") String retrieve_type,
                                                        @Query("category") String category,
                                                        @Query("limit") int limit,
                                                        @Query("ad") int ad);


}
