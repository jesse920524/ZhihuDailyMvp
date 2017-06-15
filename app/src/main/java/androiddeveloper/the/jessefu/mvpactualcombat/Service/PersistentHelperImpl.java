package androiddeveloper.the.jessefu.mvpactualcombat.Service;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;

import java.util.Date;
import java.util.List;

import androiddeveloper.the.jessefu.mvpactualcombat.base.BaseApplication;
import androiddeveloper.the.jessefu.mvpactualcombat.constants.MyConstants;
import androiddeveloper.the.jessefu.mvpactualcombat.model.articleDetail.ArticleDetailBean;
import androiddeveloper.the.jessefu.mvpactualcombat.model.articleDetail.ArticleDetailModelImpl;
import androiddeveloper.the.jessefu.mvpactualcombat.model.articleDetail.IArticleDetailModel;
import androiddeveloper.the.jessefu.mvpactualcombat.model.guokrNewsDetail.GuokrNewsDetailBean;
import androiddeveloper.the.jessefu.mvpactualcombat.model.guokrNewsDetail.GuokrNewsDetailModelImpl;
import androiddeveloper.the.jessefu.mvpactualcombat.model.guokrNewsDetail.IGuokrDetailModel;
import androiddeveloper.the.jessefu.mvpactualcombat.model.listener.OnDataLoadedListener;
import androiddeveloper.the.jessefu.mvpactualcombat.model.oneMomentDetail.IOneMomentDetailModel;
import androiddeveloper.the.jessefu.mvpactualcombat.model.oneMomentDetail.OneMomentDetailBean;
import androiddeveloper.the.jessefu.mvpactualcombat.model.oneMomentDetail.OneMomentDetailModelImpl;
import androiddeveloper.the.jessefu.mvpactualcombat.model.restoreArticle.IRestoreArticleModel;
import androiddeveloper.the.jessefu.mvpactualcombat.model.restoreArticle.RestoreArticleBean;
import androiddeveloper.the.jessefu.mvpactualcombat.model.restoreArticle.RestoreArticleModelImpl;
import androiddeveloper.the.jessefu.mvpactualcombat.model.restoreListItem.IRestoreListItemModel;
import androiddeveloper.the.jessefu.mvpactualcombat.model.restoreListItem.RestoreListItemBean;
import androiddeveloper.the.jessefu.mvpactualcombat.model.restoreListItem.RestoreListItemModelImpl;
import androiddeveloper.the.jessefu.mvpactualcombat.util.UtilConnection;
import androiddeveloper.the.jessefu.mvpactualcombat.util.UtilTime;

/**
 * Created by Jesse Fu on 2017-05-13.
 */

public class PersistentHelperImpl implements IPersistentHelper, OnDataLoadedListener, IGuokrDetailModel.OnGuokrNewsDetailLoadedListener {
    private static final String TAG = PersistentHelperImpl.class.getSimpleName();
    private static final int CLEAR_EXPIRE_DATA = 0;

    private IRestoreArticleModel modelRestoreArticle;
    private IRestoreListItemModel modelRestoreListItem;
    private IArticleDetailModel modelZHDetail;
    private IOneMomentDetailModel modelOMDetail;
    private IGuokrDetailModel modelGKDetail;
    private SharedPreferences sp;

    private PersistentHandler handler;

    public PersistentHelperImpl() {
        modelRestoreArticle = new RestoreArticleModelImpl();
        modelRestoreListItem = new RestoreListItemModelImpl();
        modelZHDetail = new ArticleDetailModelImpl();
        modelOMDetail = new OneMomentDetailModelImpl();
        modelGKDetail = new GuokrNewsDetailModelImpl();
        handler = new PersistentHandler();
        sp = BaseApplication.getContext().getSharedPreferences(MyConstants.USER_SETTINGS, Context.MODE_PRIVATE);
    }

    @Override
    public List<RestoreListItemBean> getPersistentList() {
        return modelRestoreListItem.queryList();
    }

    @Override
    public List<RestoreListItemBean> getPersistentList(String type) {
        return modelRestoreListItem.queryList(type);
    }

    @Override
    public void getZHDetail(Long id) {
        modelZHDetail.getArticleDetail(this, String.valueOf(id));
    }

    @Override
    public void getOMDetail(Long id) {
        modelOMDetail.getOneMomentDetailBean(this, String.valueOf(id));
    }

    @Override
    public void getGuokrDetail(Long id) {
        modelGKDetail.getGuokrNewsDetail(this, String.valueOf(id));
    }

    @Override
    public List<ArticleDetailBean> getZHDetail(List<RestoreListItemBean> restoreListItemBeanList) {
        return null;
    }

    @Override
    public List<OneMomentDetailBean> getOMDetail(List<RestoreListItemBean> restoreListItemBeanList) {
        return null;
    }

    @Override
    public List<GuokrNewsDetailBean> getGuokrDetail(List<RestoreListItemBean> restoreListItemBeanList) {
        return null;
    }

    @Override
    public void clearExpireData() {
        String cacheLife = sp.getString("cache_life", "");
        Log.d(TAG, "cache life: " + cacheLife);
        Message message = Message.obtain();
        message.what = CLEAR_EXPIRE_DATA;
        switch (cacheLife){
            case "从不缓存数据":
                message.arg1 = 0;
                break;
            case "":
            case "3天":
                message.arg1 = 3;
                break;
            case "7天":
                message.arg1 = 7;
                break;
            default:
                break;
        }
        handler.sendMessage(message);
    }

    @Override
    public void persistentData() {
        try {
        if (UtilConnection.getWifiState()) {

                List<RestoreListItemBean> list = this.getPersistentList();
                for (RestoreListItemBean itemBean : list) {
                    switch (itemBean.getArticleType()) {
                        case MyConstants.ARTICLE_TYPE_ZHIHU:
                            this.getZHDetail(itemBean.getArticleId());
                            break;
                        case MyConstants.ARTICLE_TYPE_ONEMOMENT:
                            this.getOMDetail(itemBean.getArticleId());
                            break;
                        case MyConstants.ARTICLE_TYPE_GUOKR:
                            this.getGuokrDetail(itemBean.getArticleId());
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Log.d(TAG, "全部持久化完成, stopself");
        }
    }


    @Override
    public void start() {
        Log.d(TAG, "start exec.");
        clearExpireData();
    }

    @Override
    public void onSuccessZH(ArticleDetailBean articleDetailBean) {
        RestoreArticleBean restoreArticleBean = new RestoreArticleBean();
        restoreArticleBean.setDate(Long.valueOf(UtilTime.get8StringDate(new Date())));
        restoreArticleBean.setArticleId(Long.valueOf(articleDetailBean.getId()));
        restoreArticleBean.setArticleType(MyConstants.ARTICLE_TYPE_ZHIHU);
        restoreArticleBean.setArticleDetail(new Gson().toJson(articleDetailBean));
        modelRestoreArticle.saveArticle(restoreArticleBean);
    }

    @Override
    public void onSuccessOM(OneMomentDetailBean oneMomentDetailBean) {
        RestoreArticleBean restoreArticleBean = new RestoreArticleBean();
        restoreArticleBean.setDate(Long.valueOf(UtilTime.get8StringDate(new Date())));
        restoreArticleBean.setArticleId(Long.valueOf(oneMomentDetailBean.getId()));
        restoreArticleBean.setArticleType(MyConstants.ARTICLE_TYPE_ONEMOMENT);
        restoreArticleBean.setArticleDetail(new Gson().toJson(oneMomentDetailBean));
        modelRestoreArticle.saveArticle(restoreArticleBean);
    }

    @Override
    public void onSuccessGK(String detailBean, String articleId) {
        RestoreArticleBean restoreArticleBean = new RestoreArticleBean();
        restoreArticleBean.setDate(Long.valueOf(UtilTime.get8StringDate(new Date())));
        restoreArticleBean.setArticleId(Long.valueOf(articleId));
        restoreArticleBean.setArticleType(MyConstants.ARTICLE_TYPE_GUOKR);
        restoreArticleBean.setArticleDetail(detailBean);
        modelRestoreArticle.saveArticle(restoreArticleBean);
    }

    @Override
    public void onError(String errMsg) {

    }

    class PersistentHandler extends Handler{

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case CLEAR_EXPIRE_DATA:
                    modelRestoreArticle.deleteExpireArticles(msg.arg1);
                    modelRestoreListItem.deleteExpireItems(msg.arg1);
                    persistentData();
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    }
}
