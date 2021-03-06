package androiddeveloper.the.jessefu.mvpactualcombat.biz.webView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.gson.Gson;

import java.io.Serializable;
import java.util.Date;

import androiddeveloper.the.jessefu.mvpactualcombat.anotations.HttpRequest;
import androiddeveloper.the.jessefu.mvpactualcombat.base.BaseApplication;
import androiddeveloper.the.jessefu.mvpactualcombat.constants.MyConstants;
import androiddeveloper.the.jessefu.mvpactualcombat.model.articleDetail.ArticleDetailBean;
import androiddeveloper.the.jessefu.mvpactualcombat.model.articleDetail.ArticleDetailModelImpl;
import androiddeveloper.the.jessefu.mvpactualcombat.model.articleDetail.IArticleDetailModel;
import androiddeveloper.the.jessefu.mvpactualcombat.model.guokrNewsDetail.GuokrNewsDetailModelImpl;
import androiddeveloper.the.jessefu.mvpactualcombat.model.guokrNewsDetail.IGuokrDetailModel;
import androiddeveloper.the.jessefu.mvpactualcombat.model.listener.OnDataLoadedListener;
import androiddeveloper.the.jessefu.mvpactualcombat.model.oneMomentDetail.IOneMomentDetailModel;
import androiddeveloper.the.jessefu.mvpactualcombat.model.oneMomentDetail.OneMomentDetailBean;
import androiddeveloper.the.jessefu.mvpactualcombat.model.oneMomentDetail.OneMomentDetailModelImpl;
import androiddeveloper.the.jessefu.mvpactualcombat.model.restoreArticle.IRestoreArticleModel;
import androiddeveloper.the.jessefu.mvpactualcombat.model.restoreArticle.RestoreArticleBean;
import androiddeveloper.the.jessefu.mvpactualcombat.model.restoreArticle.RestoreArticleModelImpl;
import androiddeveloper.the.jessefu.mvpactualcombat.common.util.UtilConnection;
import androiddeveloper.the.jessefu.mvpactualcombat.common.util.UtilTime;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by Jesse Fu on 2017/3/3 0003.
 */

public class WebviewPresenter implements WebviewContract.IWebviewPresenter,
        OnDataLoadedListener, IGuokrDetailModel.OnGuokrNewsDetailLoadedListener {

    private static final String TAG = WebviewPresenter.class.getSimpleName();

    private SharedPreferences sp;

    private WebviewContract.IWebviewView view;
    private IArticleDetailModel modelZhihuArticle;
    private IOneMomentDetailModel modelOneMomentDetail;
    private IGuokrDetailModel modelGuokrDetail;

    private IRestoreArticleModel modelRestoreArticle;

    public WebviewPresenter(WebviewContract.IWebviewView view) {
        //获取sharedPreference,判断是否无图模式
        sp = BaseApplication.getContext().getSharedPreferences(MyConstants.USER_SETTINGS, MODE_PRIVATE);

        this.view = view;
        modelZhihuArticle = new ArticleDetailModelImpl();
        modelOneMomentDetail = new OneMomentDetailModelImpl();
        modelRestoreArticle  = new RestoreArticleModelImpl();
        modelGuokrDetail = new GuokrNewsDetailModelImpl();
        view.setPresenter(this);

    }

    /**
     * start()方法约定了所有presenter的初始化操作都放在start（）中
     */
    @Override
    public void start() {
        view.showLoading();
        getData(view.getArticleId(view.getActivityIntent()), view.getArticleType(view.getActivityIntent()));
    }

    /**首先查询db数据,若有数据,优先加载.
     * 若无数据,发送请求
     * 获取文章详情,需要文章id*/
    @HttpRequest(httpMethod = "get")
    @Override
    public void getData(String receivedId, String receivedType) {
            Log.d(TAG, "getZhihuArticleDetail: " + receivedId + " " + receivedType);
            RestoreArticleBean bean = modelRestoreArticle.queryArticleById(Long.valueOf(receivedId));

            if (null == bean){
                switch (receivedType){
                    case MyConstants.ARTICLE_TYPE_ZHIHU:
                        modelZhihuArticle.getArticleDetail(this, receivedId);
                        break;
                    case MyConstants.ARTICLE_TYPE_GUOKR:
                        modelGuokrDetail.getGuokrNewsDetail(this, receivedId);
                        break;
                    case MyConstants.ARTICLE_TYPE_ONEMOMENT:
                        modelOneMomentDetail.getOneMomentDetailBean(this, receivedId);
                        break;
                }
            }else {
                Log.d(TAG, "读取本地持久化数据: " + bean.toString());
                switch (bean.getArticleType()){
                    case MyConstants.ARTICLE_TYPE_ZHIHU:
                        onSuccessZH(new Gson().fromJson(bean.getArtticleDetail(), ArticleDetailBean.class));
                        break;
                    case MyConstants.ARTICLE_TYPE_ONEMOMENT:
                        onSuccessOM(new Gson().fromJson(bean.getArtticleDetail(), OneMomentDetailBean.class));
                        break;
                    case MyConstants.ARTICLE_TYPE_GUOKR:
                        onSuccessGK(bean.getArtticleDetail(), receivedId);
                        break;
                }
            }
    }

    @Override
    public boolean checkNoPicMode() {
        return sp.getBoolean("no_pic_mode", false)?true : false;
    }

    @Override
    public void share(String type, String url, String title) {
        /**检查文章类型合法性*/
        try{
            checkArticleType(type);
        }catch (Exception e){
            BaseApplication.showToast(e.getLocalizedMessage());
            return;
        }

        try{
            Intent shareIntent = new Intent(Intent.ACTION_SEND).setType("text/plain");
            StringBuilder sb = new StringBuilder();
            sb.append("分享自 『Material Daily』:");
            sb.append("\n").append(title).append(" ");

            switch (type){
                case MyConstants.ARTICLE_TYPE_ZHIHU:
                case MyConstants.ARTICLE_TYPE_GUOKR:
                    sb.append(url);
                    break;
                case MyConstants.ARTICLE_TYPE_ONEMOMENT:
                    sb.append(url);
                    break;
            }
            sb.append("\t\t\t");
            shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
            ((AppCompatActivity)view).startActivity(Intent.createChooser(shareIntent, "to: "));
        }catch (Exception e){
            e.printStackTrace();
        }


    }

    @Override
    public boolean checkArticleType(String type) {
        if (type.equals(MyConstants.ARTICLE_TYPE_ONEMOMENT)
                ||type.equals(MyConstants.ARTICLE_TYPE_ZHIHU)
                ||type.equals(MyConstants.ARTICLE_TYPE_GUOKR))
            return true;
        throw new IllegalArgumentException("Illegal ArticleType!");
    }

    /**
     * 持久化文章详情*/
    @Override
    public <T extends Serializable> void saveArticle(T t, String type) {
        String articleDetail = null;
        switch (type){
            case MyConstants.ARTICLE_TYPE_ZHIHU:
            case MyConstants.ARTICLE_TYPE_ONEMOMENT:
                articleDetail = new Gson().toJson(t);
                Log.d(TAG +  "persistent: ", articleDetail);
                break;
            case MyConstants.ARTICLE_TYPE_GUOKR:
                articleDetail = (String) t;
                break;
        }

        Long articleId = Long.valueOf(view.getArticleId(view.getActivityIntent()));
        String articleType = type;
        Long date = Long.valueOf(UtilTime.dateToStr(new Date()).replace("-", ""));
        RestoreArticleBean  restoreArticleBean = new RestoreArticleBean(date, articleId, articleType, articleDetail);
//        Log.d(TAG, "restoreArticleBean :" + restoreArticleBean.toString());
        modelRestoreArticle.saveArticle(restoreArticleBean);
    }



    @Override
    public RestoreArticleBean getArticleFromDb(Long id) {
            return modelRestoreArticle.queryArticleById(id);
    }

    @Override
    public void onSuccessZH(ArticleDetailBean articleDetailBean) {
        view.dismissLoading();
        view.getZhihuArticleDetail(articleDetailBean);
        saveArticle(articleDetailBean, MyConstants.ARTICLE_TYPE_ZHIHU);
    }

    @Override
    public void onSuccessOM(OneMomentDetailBean bean) {
        view.dismissLoading();
        view.getOneMomentDetail(bean);
        saveArticle(bean, MyConstants.ARTICLE_TYPE_ONEMOMENT);
    }

    @Override
    public void onSuccessGK(String detailBean, String articleId) {
        view.dismissLoading();
        view.getGuokrNewsDetail(detailBean);
        saveArticle(detailBean, MyConstants.ARTICLE_TYPE_GUOKR);
    }

    @Override
    public void onError(String errMsg) {
        view.dismissLoading();
        if (!UtilConnection.getNetworkState()){
            view.showErrorSnack();
        }view.dismissLoading();
        if (!UtilConnection.getNetworkState()){
            view.showErrorSnack();
        }
    }
}
