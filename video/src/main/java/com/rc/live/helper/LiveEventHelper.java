package com.rc.live.helper;


import static com.rc.live.constant.CurrentStatusType.STATUS_NOT_ON_SEAT;
import static com.rc.live.constant.CurrentStatusType.STATUS_ON_SEAT;
import static com.rc.live.constant.CurrentStatusType.STATUS_WAIT_FOR_SEAT;
import static com.rc.live.constant.InviteStatusType.STATUS_CONNECTTING;
import static com.rc.live.constant.InviteStatusType.STATUS_NOT_INVITRED;
import static com.rc.live.constant.InviteStatusType.STATUS_UNDER_INVITATION;

import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import com.basis.net.oklib.OkApi;
import com.basis.net.oklib.OkParams;
import com.basis.net.oklib.WrapperCallBack;
import com.basis.net.oklib.wrapper.Wrapper;
import com.basis.ui.UIStack;
import com.basis.utils.KToast;
import com.basis.utils.Logger;
import com.basis.wapper.IResultBack;
import com.basis.wapper.IRoomCallBack;
import com.basis.widget.dialog.VRCenterDialog;
import com.meihu.beauty.utils.MhDataManager;
import com.rc.live.constant.CurrentStatusType;
import com.rc.live.constant.InviteStatusType;
import com.rc.live.room.LiveRoomKvKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import cn.rongcloud.config.UserManager;
import cn.rongcloud.config.provider.user.User;
import cn.rongcloud.config.provider.user.UserProvider;
import cn.rongcloud.liveroom.api.RCHolder;
import cn.rongcloud.liveroom.api.RCLiveEngine;
import cn.rongcloud.liveroom.api.RCLiveMixType;
import cn.rongcloud.liveroom.api.RCLiveSeatViewProvider;
import cn.rongcloud.liveroom.api.RCParamter;
import cn.rongcloud.liveroom.api.callback.RCLiveCallback;
import cn.rongcloud.liveroom.api.callback.RCLiveResultCallback;
import cn.rongcloud.liveroom.api.error.RCLiveError;
import cn.rongcloud.liveroom.api.interfaces.RCLiveEventListener;
import cn.rongcloud.liveroom.api.interfaces.RCLiveLinkListener;
import cn.rongcloud.liveroom.api.interfaces.RCLivePKListener;
import cn.rongcloud.liveroom.api.interfaces.RCLiveSeatListener;
import cn.rongcloud.liveroom.api.model.RCLiveSeatInfo;
import cn.rongcloud.liveroom.api.model.RCLiveVideoPK;
import cn.rongcloud.liveroom.api.model.RCLivevideoFinishReason;
import cn.rongcloud.liveroom.manager.RCDataManager;
import cn.rongcloud.music.MusicControlManager;
import cn.rongcloud.pk.PKManager;
import cn.rongcloud.pk.bean.PKInviteInfo;
import cn.rongcloud.pk.bean.PKResponse;
import cn.rongcloud.roomkit.api.VRApi;
import cn.rongcloud.roomkit.manager.RCChatRoomMessageManager;
import cn.rongcloud.roomkit.message.RCChatroomAdmin;
import cn.rongcloud.roomkit.message.RCChatroomBarrage;
import cn.rongcloud.roomkit.message.RCChatroomEnter;
import cn.rongcloud.roomkit.message.RCChatroomGift;
import cn.rongcloud.roomkit.message.RCChatroomGiftAll;
import cn.rongcloud.roomkit.message.RCChatroomKickOut;
import cn.rongcloud.roomkit.message.RCChatroomLocationMessage;
import cn.rongcloud.roomkit.message.RCChatroomSeats;
import cn.rongcloud.roomkit.message.RCChatroomVoice;
import cn.rongcloud.roomkit.message.RCFollowMsg;
import cn.rongcloud.roomkit.ui.miniroom.MiniRoomManager;
import cn.rongcloud.roomkit.ui.miniroom.OnCloseMiniRoomListener;
import cn.rongcloud.roomkit.ui.room.fragment.ClickCallback;
import cn.rongcloud.roomkit.ui.room.model.MemberCache;
import cn.rongcloud.rtc.api.RCRTCConfig;
import cn.rongcloud.rtc.api.RCRTCMixConfig;
import cn.rongcloud.rtc.base.RCRTCVideoFrame;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.UserInfo;
import io.rong.message.TextMessage;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

/**
 * @author lihao
 * @project RongRTCDemo
 * @date 2021/11/16
 * @time 5:30 ??????
 * ????????????????????????????????????  ???????????? ???????????????
 * ????????????????????????????????????
 */
public class LiveEventHelper implements ILiveEventHelper, RCLiveEventListener, RCLiveLinkListener, RCLiveSeatListener, RCLivePKListener, OnCloseMiniRoomListener {

    private String TAG = "LiveEventHelper";

    private List<MessageContent> messageList = new ArrayList<>();
    private String roomId;//??????????????????ID
    private String createUserId;//???????????????ID
    private CurrentStatusType currentStatus = STATUS_NOT_ON_SEAT;
    private CurrentStatusType lastStatus = STATUS_NOT_ON_SEAT;
    private InviteStatusType inviteStatusType = STATUS_NOT_INVITRED;
    private List<LiveRoomListener> liveRoomListeners = new ArrayList<>();
    private VRCenterDialog pickReceivedDialog;
    //????????????????????????
    private boolean isMute = false;
    private SparseArray<RCHolder> holder = new SparseArray<>(16);

    public boolean isMute() {
        return isMute;
    }

    public static LiveEventHelper getInstance() {
        return helper.INSTANCE;
    }

    @Override
    public CurrentStatusType getCurrentStatus() {
        return currentStatus;
    }

    @Override
    public void setCurrentStatus(CurrentStatusType currentStatus) {
        this.lastStatus = this.currentStatus;
        this.currentStatus = currentStatus;
    }

    public InviteStatusType getInviteStatusType() {
        return inviteStatusType;
    }

    public void setInviteStatusType(InviteStatusType inviteStatusType) {
        Logger.e(TAG, "setInviteStatusType: inviteStatusType = " + inviteStatusType);
        this.inviteStatusType = inviteStatusType;
    }

    @Override
    public void register(String roomId) {
        this.roomId = roomId;
        RCLiveEngine.getInstance().setLiveEventListener(this);
        RCLiveEngine.getInstance().getLinkManager().setLiveLinkListener(this);
        RCLiveEngine.getInstance().getSeatManager().setLiveSeatListener(this);
        RCLiveEngine.getInstance().setLivePKEventListener(this);
        setSeatViewProvider();
    }

    @Override
    public RCHolder getHold(int index) {
        if (holder.size() > index) {
            return holder.get(index);
        }
        return null;
    }

    @Override
    public void onSeatSpeak(RCLiveSeatInfo seatInfo, int audioLevel) {
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onSeatSpeak(seatInfo, audioLevel);
        }
    }

    @Override
    public void unRegister() {
        this.roomId = null;
        this.createUserId = null;
        setCurrentStatus(STATUS_NOT_ON_SEAT);
        setInviteStatusType(STATUS_NOT_INVITRED);
        messageList.clear();
        RCLiveEngine.getInstance().unPrepare(null);
        isMute = false;
        holder.clear();
        removeSeatViewProvider();
        PKManager.get().unInit();
        MusicControlManager.getInstance().release();
    }

    /**
     * ??????provider
     */
    public void setSeatViewProvider() {
        RCLiveEngine.getInstance().setSeatViewProvider(new RCLiveSeatViewProvider() {

            @Override
            public void convert(RCHolder holder, RCLiveSeatInfo seat, RCParamter parameter) {
                for (LiveRoomListener liveRoomListener : liveRoomListeners) {
                    liveRoomListener.onBindView(holder, seat, parameter);
                }
            }

            @Override
            public View inflate(RCLiveSeatInfo seatInfo, RCParamter rcParamter) {
                for (LiveRoomListener liveRoomListener : liveRoomListeners) {
                    return liveRoomListener.inflaterSeatView(seatInfo, rcParamter);
                }
                return null;
            }

            @Override
            public void onListenerHolds(SparseArray<RCHolder> rcHolderSparseArray) {
                holder = rcHolderSparseArray;
            }
        });
    }

    /**
     * ??????provider
     */
    public void removeSeatViewProvider() {
        RCLiveEngine.getInstance().setSeatViewProvider(null);
    }


    public void setCreateUserId(String createUserId) {
        this.createUserId = createUserId;
    }

    @Override
    public void leaveRoom(IRoomCallBack callback) {
        RCLiveEngine.getInstance().leaveRoom(new RCLiveCallback() {
            @Override
            public void onSuccess() {
                MusicControlManager.getInstance().stopPlayMusic();
                unRegister();
                changeUserRoom("");
                if (callback != null)
                    callback.onSuccess();
            }

            @Override
            public void onError(int code, RCLiveError error) {
                if (callback != null)
                    callback.onError(code, error.getMessage());
            }
        });
    }

    @Override
    public void joinRoom(String roomId, ClickCallback<Boolean> callback) {
        register(roomId);
        RCLiveEngine.getInstance().joinRoom(roomId, new RCLiveCallback() {
            @Override
            public void onSuccess() {
                changeUserRoom(roomId);
                if (callback != null)
                    callback.onResult(true, "??????????????????");
                Log.e(TAG, "onError: joinRoom Success:");
            }

            @Override
            public void onError(int code, RCLiveError error) {
                if (callback != null)
                    callback.onResult(false, error.getMessage());
                Log.e(TAG, "onError: joinRoom Fail:" + error.getMessage());
            }
        });
    }

    /**
     * ???????????????-1?????????????????????????????????
     *
     * @param userId
     * @param index
     * @param callback
     */
    @Override
    public void pickUserToSeat(String userId, int index, ClickCallback<Boolean> callback) {
        // ???????????????????????????????????????????????????
        RCLiveEngine.getInstance().getLinkManager().inviteLiveVideo(userId, -1, new RCLiveCallback() {
            @Override
            public void onSuccess() {
                //?????????????????????
                if (RCDataManager.get().getMixType() == RCLiveMixType.RCMixTypeOneToOne.getValue()) {
                    setInviteStatusType(STATUS_UNDER_INVITATION);
                }
                if (callback != null) callback.onResult(true, "??????????????????????????????????????????");
            }

            @Override
            public void onError(int code, RCLiveError error) {
                if (callback != null) callback.onResult(false, "?????????????????????");
            }
        });
    }

    /**
     * ??????????????????
     */
    @Override
    public void cancelInvitation(String userId, ClickCallback<Boolean> callback) {
        RCLiveEngine.getInstance().getLinkManager().cancelInvitation(userId, new RCLiveCallback() {
            @Override
            public void onSuccess() {
                setInviteStatusType(STATUS_NOT_INVITRED);
                if (callback != null) callback.onResult(true, "??????????????????????????????");
            }

            @Override
            public void onError(int code, RCLiveError error) {
                if (callback != null) callback.onResult(true, "????????????????????????");
            }
        });
    }

    /**
     * ?????????????????????????????????????????????????????????SDK ?????????????????????????????????
     *
     * @param userId   ????????????id
     * @param callback ????????????
     */
    @Override
    public void acceptRequestSeat(String userId, ClickCallback<Boolean> callback) {
        RCLiveEngine.getInstance().getLinkManager().acceptRequest(userId, new RCLiveCallback() {
            @Override
            public void onSuccess() {
                if (callback != null)
                    callback.onResult(true, "????????????????????????");
            }

            @Override
            public void onError(int code, RCLiveError error) {
                if (callback != null)
                    callback.onResult(false, "??????????????????");
            }
        });
    }

    /**
     * ???????????????????????????
     *
     * @param userId
     * @param callback
     */
    @Override
    public void rejectRequestSeat(String userId, ClickCallback<Boolean> callback) {
        RCLiveEngine.getInstance().getLinkManager().rejectRequest(userId, new RCLiveCallback() {
            @Override
            public void onSuccess() {
                if (callback != null)
                    callback.onResult(true, "????????????????????????");
                MemberCache.getInstance().refreshMemberData(roomId);
            }

            @Override
            public void onError(int code, RCLiveError error) {
                if (callback != null)
                    callback.onResult(false, "??????????????????????????????:" + error.getMessage());
            }
        });
    }

    @Override
    public void cancelRequestSeat(ClickCallback<Boolean> callback) {
        RCLiveEngine.getInstance().getLinkManager().cancelRequest(new RCLiveCallback() {
            @Override
            public void onSuccess() {
                Logger.e(TAG, "cancelRequestSeat");
                if (callback != null) {
                    setCurrentStatus(STATUS_NOT_ON_SEAT);
                    callback.onResult(true, "????????????????????????");
                }

            }

            @Override
            public void onError(int code, RCLiveError error) {
                if (callback != null)
                    callback.onResult(false, "????????????????????????:" + error.getMessage());
            }
        });
    }

    @Override
    public void lockSeat(int index, boolean isClose, ClickCallback<Boolean> callback) {
        RCLiveEngine.getInstance().getSeatManager().lock(index, isClose, new RCLiveCallback() {
            @Override
            public void onSuccess() {
                //???????????????
                if (callback != null)
                    callback.onResult(true, isClose ? "???????????????" : "???????????????");
            }

            @Override
            public void onError(int code, RCLiveError error) {
                //???????????????
                if (callback != null) callback.onResult(false, error.getMessage());
            }
        });
    }

    @Override
    public void switchToSeat(int seatIndex, ClickCallback<Boolean> callback) {
        RCLiveEngine.getInstance().switchTo(seatIndex, new RCLiveCallback() {
            @Override
            public void onSuccess() {
                if (callback != null) callback.onResult(true, "??????????????????");
            }

            @Override
            public void onError(int code, RCLiveError error) {
                if (callback != null) callback.onResult(false, error.getMessage());
            }
        });
    }

    @Override
    public void muteSeat(int index, boolean isMute, ClickCallback<Boolean> callback) {
        RCLiveEngine.getInstance().getSeatManager().mute(index, isMute, new RCLiveCallback() {
            @Override
            public void onSuccess() {
                //??????????????????
                if (callback != null) callback.onResult(true, "");
                if (isMute) {
                    KToast.show("??????????????????");
                } else {
                    KToast.show("???????????????");
                }
            }

            @Override
            public void onError(int code, RCLiveError error) {
                //??????????????????
                if (callback != null) callback.onResult(false, error.getMessage());
            }
        });
    }

    @Override
    public void switchVideoOrAudio(int index, boolean isVideo, ClickCallback<Boolean> callback) {
        RCLiveEngine.getInstance().getSeatManager().enableVideo(index, isVideo, new RCLiveCallback() {
            @Override
            public void onSuccess() {
                if (callback != null) callback.onResult(true, "");
                if (isVideo) {
                    KToast.show("???????????????????????????");
                } else {
                    KToast.show("???????????????????????????");
                }
            }

            @Override
            public void onError(int code, RCLiveError error) {
                if (callback != null) callback.onResult(false, error.getMessage());
            }
        });
    }

    @Override
    public void MuteSelf(int index, boolean isMute, ClickCallback<Boolean> callback) {
        this.isMute = isMute;
        RCLiveEngine.getInstance().getSeatManager().enableAudio(index, !isMute, new RCLiveCallback() {
            @Override
            public void onSuccess() {
                if (callback != null) callback.onResult(true, "?????????????????????");
            }

            @Override
            public void onError(int code, RCLiveError error) {
                if (callback != null) callback.onResult(true, "?????????????????????");
            }
        });
    }

    @Override
    public void kickUserFromRoom(User user, ClickCallback<Boolean> callback) {
        RCLiveEngine.getInstance().kickOutRoom(user.getUserId(), new RCLiveCallback() {
            @Override
            public void onSuccess() {
                if (callback != null) callback.onResult(true, "????????????");
            }

            @Override
            public void onError(int code, RCLiveError error) {
                if (callback != null) callback.onResult(false, error.getMessage());
            }
        });
    }


    /**
     * ????????????
     *
     * @param user
     * @param callback
     */
    @Override
    public void kickUserFromSeat(User user, ClickCallback<Boolean> callback) {
        RCLiveEngine.getInstance().kickOutSeat(user.getUserId(), new RCLiveCallback() {
            @Override
            public void onSuccess() {
                if (callback != null) callback.onResult(true, "??????????????????");
            }

            @Override
            public void onError(int code, RCLiveError error) {
                if (callback != null) callback.onResult(false, error.getMessage());
            }
        });
    }

    @Override
    public void changeUserRoom(String roomId) {
        HashMap<String, Object> params = new OkParams()
                .add("roomId", roomId)
                .build();
        OkApi.get(VRApi.USER_ROOM_CHANGE, params, new WrapperCallBack() {
            @Override
            public void onResult(Wrapper result) {
                if (result.ok()) {
                    Log.e(TAG, "onResult: " + result.getMessage());
                }
            }
        });
    }

    /**
     * ??????SDK?????????????????????????????????????????????????????????????????????????????????????????????????????????API????????????
     *
     * @param callback
     */
    @Override
    public void finishRoom(ClickCallback<Boolean> callback) {
        RCLiveEngine.getInstance().finish(new RCLiveCallback() {
            @Override
            public void onSuccess() {
                unRegister();
                changeUserRoom("");
                if (callback != null)
                    callback.onResult(true, "????????????");
            }

            @Override
            public void onError(int code, RCLiveError error) {
                if (callback != null)
                    callback.onResult(false, "????????????");
            }
        });
    }

    @Override
    public void begin(String roomId, ClickCallback<Boolean> callback) {
        register(roomId);
        RCLiveEngine.getInstance().begin(roomId, new RCLiveCallback() {
            @Override
            public void onSuccess() {
                //????????????????????????????????????
                Log.e(TAG, "onSuccess: ");
                changeUserRoom(roomId);
                if (callback != null)
                    callback.onResult(true, "??????????????????");
            }

            @Override
            public void onError(int code, RCLiveError error) {
                Log.e("TAG", "onError: " + code);
                if (callback != null)
                    callback.onResult(false, "??????????????????" + ":" + code);
            }
        });
    }

    @Override
    public void prepare(ClickCallback<Boolean> callback) {
        RCLiveEngine.getInstance().prepare(new RCLiveCallback() {
            @Override
            public void onSuccess() {
                if (callback != null)
                    callback.onResult(true, "??????????????????");
            }

            @Override
            public void onError(int code, RCLiveError error) {
                KToast.show(error.getMessage());
            }
        });
    }

    @Override
    public void requestLiveVideo(int index, ClickCallback<Boolean> callback) {
        RCLiveEngine.getInstance().getLinkManager().requestLiveVideo(index, new RCLiveCallback() {
            @Override
            public void onSuccess() {
                setCurrentStatus(STATUS_WAIT_FOR_SEAT);
                if (callback != null) {
                    callback.onResult(true, "");
                }
                KToast.show("????????????????????????????????????");
            }

            @Override
            public void onError(int code, RCLiveError error) {
                if (callback != null) {
                    callback.onResult(false, error.getMessage());
                }
                KToast.show("??????????????????");
            }
        });
    }

    @Override
    public void enterSeat(int index, ClickCallback<Boolean> callback) {
        RCLiveEngine.getInstance().enterSeat(index, new RCLiveCallback() {
            @Override
            public void onSuccess() {
                if (callback != null) callback.onResult(true, "????????????");
            }

            @Override
            public void onError(int code, RCLiveError error) {
                if (callback != null) callback.onResult(true, error.getMessage());
            }
        });
    }

    @Override
    public void leaveSeat(ClickCallback<Boolean> callback) {
        RCLiveEngine.getInstance().leaveSeat(new RCLiveCallback() {
            @Override
            public void onSuccess() {
                if (callback != null) callback.onResult(true, "??????????????????");
            }

            @Override
            public void onError(int code, RCLiveError error) {
                if (callback != null) callback.onResult(false, error.getMessage());
            }
        });
    }

    @Override
    public void updateRoomInfoKv(String key, String vaule, ClickCallback<Boolean> callback) {
        Map<String, String> kv = new HashMap<>();
        kv.put(key, vaule);
        RCLiveEngine.getInstance().setRoomInfo(kv, new RCLiveCallback() {
            @Override
            public void onSuccess() {
                if (callback != null) {
                    callback.onResult(true, "??????" + key + "??????");
                }
            }

            @Override
            public void onError(int code, RCLiveError error) {
                if (callback != null) {
                    callback.onResult(false, "??????" + key + "??????:" + error.getMessage());
                }
            }
        });
    }

    /**
     * ??????KV??????
     *
     * @param key
     * @param callback
     */
    @Override
    public void getRoomInfoByKey(String key, ClickCallback<Boolean> callback) {
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<String> emitter) throws Throwable {
                RCLiveEngine.getInstance().getRoomInfo(key, new RCLiveResultCallback<String>() {
                    @Override
                    public void onResult(String vaule) {
                        if (TextUtils.equals(LiveRoomKvKey.LIVE_ROOM_ENTER_SEAT_MODE, key)) {
                            //???????????????????????????
                            if (TextUtils.isEmpty(vaule)) {
                                //?????????????????????
                                emitter.onNext("0");
                                return;
                            }
                        }
                        emitter.onNext(vaule);
                    }

                    @Override
                    public void onError(int code, RCLiveError error) {
                        emitter.onError(new Throwable(error.getMessage()));
                    }
                });
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String vaule) throws Throwable {
                        if (callback != null) {
                            callback.onResult(true, vaule);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Throwable {
                        if (callback != null) {
                            callback.onResult(false, throwable.getMessage());
                        }
                    }
                });
    }

    @Override
    public void getRoomInfoByKey(List<String> keys, ClickCallback<Map<String, String>> callback) {
        Observable.create(new ObservableOnSubscribe<Map<String, String>>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Map<String, String>> emitter) throws Throwable {
                RCLiveEngine.getInstance().getRoomInfos(keys, new RCLiveResultCallback<Map<String, String>>() {

                    @Override
                    public void onResult(Map<String, String> result) {
                        emitter.onNext(result);
                    }


                    @Override
                    public void onError(int code, RCLiveError error) {
                        emitter.onError(new Throwable(error.getMessage()));
                    }
                });
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Map<String, String>>() {
                    @Override
                    public void accept(Map<String, String> stringStringMap) throws Throwable {
                        if (callback != null) {
                            callback.onResult(stringStringMap, "????????????");
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Throwable {
                        if (callback != null) {
                            callback.onResult(null, throwable.getMessage());
                        }
                    }
                });
    }

    /**
     * ????????????????????????
     *
     * @param callback
     */
    @Override
    public void getRequestLiveVideoIds(ClickCallback<List<String>> callback) {
        RCLiveEngine.getInstance().getLinkManager().getRequestLiveVideoIds(new RCLiveResultCallback<List<String>>() {
            @Override
            public void onResult(List<String> result) {
                for (LiveRoomListener liveRoomListener : liveRoomListeners) {
                    liveRoomListener.onRequestLiveVideoIds(result);
                }
                if (callback != null) {
                    callback.onResult(result, "");
                }
            }

            @Override
            public void onError(int code, RCLiveError error) {

            }
        });
    }

    public List<MessageContent> getMessageList() {
        return messageList;
    }

    public String getRoomId() {
        return roomId;
    }

    /**
     * ??????????????????????????????
     *
     * @param liveRoomListener
     */
    public void addLiveRoomListeners(LiveRoomListener liveRoomListener) {
        Log.e(TAG, "addLiveRoomListeners: ");
        liveRoomListeners.add(liveRoomListener);
    }

    /**
     * ???????????????fragment??????
     */
    public void removeLiveRoomListeners() {
        Log.e(TAG, "removeLiveRoomListeners: ");
        liveRoomListeners.clear();
    }

    /**
     * ????????????
     *
     * @param messageContent ?????????
     * @param isShowLocation ?????????????????????
     */
    @Override
    public void sendMessage(MessageContent messageContent, boolean isShowLocation) {
        if (!TextUtils.isEmpty(roomId))
            if (messageContent instanceof RCChatroomLocationMessage) {
                RCChatRoomMessageManager.sendLocationMessage(roomId, messageContent);
                if (isShowingMessage(messageContent)) {
                    messageList.add(messageContent);
                }
            } else {
                RCChatRoomMessageManager.sendChatMessage(roomId, messageContent, isShowLocation
                        , new Function1<Integer, Unit>() {
                            @Override
                            public Unit invoke(Integer integer) {
                                if (isShowLocation) {
                                    messageList.add(messageContent);
                                }
                                return null;
                            }
                        }, new Function2<IRongCoreEnum.CoreErrorCode, Integer, Unit>() {
                            @Override
                            public Unit invoke(IRongCoreEnum.CoreErrorCode coreErrorCode, Integer integer) {
                                KToast.show("????????????");
                                return null;
                            }
                        });
            }

    }

    @Override
    public void onRoomInfoReady() {
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onRoomInfoReady();
        }
        Log.e(TAG, "onRoomInfoReady: ");
    }

    /**
     * @param key   ?????????????????????kv???key
     * @param value ?????????????????????kv???value
     */
    @Override
    public void onRoomInfoUpdate(String key, String value) {
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onRoomInfoUpdate(key, value);
        }
        Log.e(TAG, "onRoomInfoUpdate: ");
    }


    @Override
    public void onUserEnter(String userId, int onlineCount) {
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onUserEnter(userId, onlineCount);
        }
        UserProvider.provider().getAsyn(userId, new IResultBack<UserInfo>() {
            @Override
            public void onResult(UserInfo userInfo) {
                RCChatroomEnter enter = new RCChatroomEnter();
                enter.setUserId(userId);
                enter.setUserName(userInfo.getName());
                Message message = Message.obtain(roomId, Conversation.ConversationType.CHATROOM, enter);
                onReceiveMessage(message);
            }
        });
        Log.e(TAG, "onUserEnter: " + userId);
    }

    @Override
    public void onUserExit(String userId, int onlineCount) {
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onUserExit(userId, onlineCount);
        }
        Log.e(TAG, "onUserExit: " + userId);
    }


    /**
     * ?????????????????????
     *
     * @param userId     ????????????????????????
     * @param operatorId ??????????????????????????????????????????
     */
    @Override
    public void onUserKickOut(String userId, String operatorId) {
        //??????????????????????????????????????????????????????
        if (TextUtils.equals(userId, UserManager.get().getUserId())) {
            KToast.show(TextUtils.equals(operatorId, createUserId) ? "????????????????????????" : "???????????????????????????");
            MiniRoomManager.getInstance().close();
            leaveRoom(null);
        }
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onUserKickOut(userId, operatorId);
        }
        Log.e(TAG, "onUserKitOut: ");
    }

    /**
     * ??????????????????
     *
     * @param lineMicUserIds ?????????????????????
     */
    @Override
    public void onLiveVideoUpdate(List<String> lineMicUserIds) {
        if (lineMicUserIds.size() == 2 && RCDataManager.get().getMixType() == RCLiveMixType.RCMixTypeOneToOne.getValue()) {
            //??????????????????????????????????????????????????????
            setInviteStatusType(STATUS_CONNECTTING);
        } else {
            setInviteStatusType(STATUS_NOT_INVITRED);
        }
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onLiveVideoUpdate(lineMicUserIds);
        }
        Log.e(TAG, "onLiveVideoUpdate: " + lineMicUserIds);
    }


    /**
     * ????????????????????????
     */
    @Override
    public void onLiveVideoRequestChange() {
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onLiveVideoRequestChange();
        }
        Log.e(TAG, "onLiveVideoRequestChanage: ");
    }

    /**
     * ???????????????????????????????????????????????????
     */
    @Override
    public void onLiveVideoRequestAccepted() {
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onLiveVideoRequestAccepted();
        }
        Log.e(TAG, "onLiveVideoRequestAccepted: ");
    }

    /**
     * ??????????????????
     */
    @Override
    public void onLiveVideoRequestRejected() {
        cancelRequestSeat(null);
        setCurrentStatus(STATUS_NOT_ON_SEAT);
        KToast.show("?????????????????????????????????");
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onLiveVideoRequestRejected();
        }
        Log.e(TAG, "onLiveVideoRequestRejected: ");
    }


    /**
     * ????????????????????????????????????????????????
     */
    @Override
    public void onLiveVideoRequestCanceled() {
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onLiveVideoRequestCanceled();
        }
        Log.e(TAG, "onLiveVideoRequestCanceled: ");
    }

    /**
     * ??????????????????
     */
    @Override
    public void onLiveVideoInvitationReceived(String userId, int index) {
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onLiveVideoInvitationReceived(userId, index);
        }
        showPickReceivedDialog(userId, index);
        Log.e(TAG, "onLiveVideoInvitationReceived: ");
    }


    /**
     * ??????????????????????????????
     *
     * @param userId
     * @param index
     */
    public void showPickReceivedDialog(String userId, int index) {
        String pickName = TextUtils.equals(userId, createUserId) ? "??????" : "?????????";
        new VRCenterDialog(UIStack.getInstance().getTopActivity(), new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {

            }
        });
        VRCenterDialog pickReceivedDialog = new VRCenterDialog(UIStack.getInstance().getTopActivity(), null);
        pickReceivedDialog.replaceContent(pickName + "??????????????????????????????? 10S", "??????", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //????????????
                RCLiveEngine.getInstance().getLinkManager().rejectInvitation(userId, null);

            }
        }, "??????", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //????????????
                RCLiveEngine.getInstance().getLinkManager().acceptInvitation(userId, index, new RCLiveCallback() {
                    @Override
                    public void onSuccess() {
                        Logger.e(TAG, "acceptInvitation:currentStatus = " + currentStatus);
                        if (currentStatus == STATUS_WAIT_FOR_SEAT || lastStatus == STATUS_WAIT_FOR_SEAT) {
                            //?????????????????????????????????????????????????????????????????????????????????????????????????????????
                            cancelRequestSeat(null);
                        }
                    }

                    @Override
                    public void onError(int code, RCLiveError error) {
                        if (error.getCode() == 80502) {
                            KToast.show("??????????????????");
                        }
                    }
                });
            }
        }, null);
        Disposable subscribe = Observable.interval(0, 1, TimeUnit.SECONDS)
                .take(11)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Throwable {
                        pickReceivedDialog.updateTitle(pickName + "??????????????????????????????? " + (10 - aLong) + "s");
                        if (10 == aLong) {
                            //??????????????????
                            RCLiveEngine.getInstance().getLinkManager().rejectInvitation(userId, null);
                            pickReceivedDialog.dismiss();
                        }
                    }
                });
        pickReceivedDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (subscribe != null) {
                    subscribe.dispose();
                }
            }
        });
        pickReceivedDialog.show();
    }

    /**
     * ???????????????
     */
    @Override
    public void onLiveVideoInvitationCanceled() {
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onLiveVideoInvitationCanceled();
        }
        if (pickReceivedDialog != null) pickReceivedDialog.dismiss();
        Log.e(TAG, "onliveVideoInvitationCanceled: ");
    }

    /**
     * ???????????????
     *
     * @param userId
     */
    @Override
    public void onLiveVideoInvitationAccepted(String userId) {
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onLiveVideoInvitationAccepted(userId);
        }
        if (TextUtils.equals(userId, UserManager.get().getUserId())) {
            KToast.show("??????????????????");
        }
        Log.e(TAG, "onLiveVideoInvitationAccepted: ");
    }

    /**
     * ???????????????
     *
     * @param userId ??????????????????
     */
    @Override
    public void onLiveVideoInvitationRejected(String userId) {
        setInviteStatusType(STATUS_NOT_INVITRED);
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onLiveVideoInvitationRejected(userId);
        }
        User member = MemberCache.getInstance().getMember(userId);
        if (member != null) {
            KToast.show("?????? " + member.getUserName() + " ???????????????");
        } else {
            KToast.show("?????? " + userId + " ???????????????");
        }
        Log.e(TAG, "onLiveVideoInvitationRejected: ");
    }

    /**
     * ????????????
     */
    @Override
    public void onLiveVideoStarted() {
        setCurrentStatus(STATUS_ON_SEAT);
        KToast.show("????????????");
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onLiveVideoStarted();
        }
        Log.e(TAG, "onLiveVideoStarted: ");
    }

    /**
     * ????????????
     */
    @Override
    public void onLiveVideoStopped(RCLivevideoFinishReason reason) {
        setCurrentStatus(STATUS_NOT_ON_SEAT);
        if (reason == RCLivevideoFinishReason.RCLivevideoFinishReasonKick) {
            KToast.show("??????????????????");
        } else if (reason == RCLivevideoFinishReason.RCLivevideoFinishReasonMix) {
            KToast.show("????????????????????????????????????");
        }
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onLiveVideoStopped(reason);
        }
        Log.e(TAG, "onLiveVideoStopped: ");
    }

    /**
     * ????????????
     *
     * @param message
     */
    @Override
    public void onReceiveMessage(Message message) {
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onReceiveMessage(message);
        }
        PKManager.get().onMessageReceived(message);
        //????????????
        if (!TextUtils.isEmpty(roomId) && message.getConversationType() == Conversation.ConversationType.CHATROOM) {
            RCChatRoomMessageManager.onReceiveMessage(roomId, message.getContent());
            if (isShowingMessage(message.getContent())) {
                messageList.add(message.getContent());
            }
        }
        Log.e(TAG, "onReceiveMessage: " + message);
    }

    @Override
    public void onNetworkStatus(long delayMs) {
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onNetworkStatus(delayMs);
        }
    }

    /**
     * ???????????????????????????????????????
     *
     * @param content
     * @return
     */
    public boolean isShowingMessage(MessageContent content) {
        if (content instanceof RCChatroomBarrage || content instanceof RCChatroomEnter
                || content instanceof RCChatroomKickOut || content instanceof RCChatroomGiftAll
                || content instanceof RCChatroomGift || content instanceof RCChatroomAdmin
                || content instanceof RCChatroomLocationMessage || content instanceof RCFollowMsg
                || content instanceof RCChatroomVoice || content instanceof TextMessage
                || content instanceof RCChatroomSeats) {
            return true;
        }
        return false;
    }

    /**
     * ????????????
     *
     * @param frame ?????????????????????
     */
    @Override
    public void onOutputSampleBuffer(RCRTCVideoFrame frame) {
        int render = MhDataManager.getInstance().render(frame.getTextureId(), frame.getWidth(), frame.getWidth());
        frame.setTextureId(render);
    }

    /**
     * RTC ????????????????????????????????????
     *
     * @param builder
     * @return ??????null?????????????????????
     */
    @Override
    public RCRTCConfig.Builder onInitRCRTCConfig(RCRTCConfig.Builder builder) {
        return null;
    }

    /**
     * RTC ?????????????????????????????????
     *
     * @param rcrtcMixConfig ????????????
     * @return ????????????null?????????????????????
     */
    @Override
    public RCRTCMixConfig onInitMixConfig(RCRTCMixConfig rcrtcMixConfig) {
        return null;
    }

    @Override
    public void onRoomMixTypeChange(RCLiveMixType mixType, int customerType) {
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onRoomMixTypeChange(mixType, customerType);
        }
        Log.e(TAG, "onRoomMixTypeChange: " + mixType);
    }


    @Override
    public void onRoomDestroy() {
        //??????????????????????????????????????????????????????
        MiniRoomManager.getInstance().close();
        unRegister();
        if (TextUtils.equals(createUserId, RongCoreClient.getInstance().getCurrentUserId())) {
            onLiveRoomFinish();
            return;
        }
        VRCenterDialog confirmDialog = new VRCenterDialog(UIStack.getInstance().getTopActivity(), null);
        confirmDialog.replaceContent("?????????????????????", "", null, "??????", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLiveRoomFinish();
            }
        }, null);
        confirmDialog.show();
        Log.e(TAG, "onRoomDestroy: ");
    }

    /**
     * ???????????????????????????
     */
    private void onLiveRoomFinish() {
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onRoomDestroy();
        }
    }


    @Override
    public void onSeatLocked(RCLiveSeatInfo seatInfo, boolean locked) {
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onSeatLocked(seatInfo, locked);
        }
    }

    @Override
    public void onSeatMute(RCLiveSeatInfo seatInfo, boolean mute) {
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onSeatMute(seatInfo, mute);
        }
    }

    @Override
    public void onSeatAudioEnable(RCLiveSeatInfo seatInfo, boolean enable) {
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onSeatAudioEnable(seatInfo, enable);
        }
    }

    @Override
    public void onSeatVideoEnable(RCLiveSeatInfo seatInfo, boolean enable) {
        for (LiveRoomListener liveRoomListener : liveRoomListeners) {
            liveRoomListener.onSeatVideoEnable(seatInfo, enable);
        }
    }

    @Override
    public void onCloseMiniRoom(OnCloseMiniRoomListener.CloseResult closeResult) {
        leaveRoom(new IRoomCallBack() {
            @Override
            public void onSuccess() {
                if (closeResult != null) {
                    closeResult.onClose();
                }
            }

            @Override
            public void onError(int code, String message) {

            }
        });
    }

    @Override
    public void onPKBegin(RCLiveVideoPK rcLiveVideoPK) {
        PKInviteInfo pkInviteInfo = new PKInviteInfo(rcLiveVideoPK.getInviterUserId(), rcLiveVideoPK.getInviterRoomId(), rcLiveVideoPK.getInviteeUserId(), rcLiveVideoPK.getInviteeRoomId());
        PKManager.get().onPKBegin(pkInviteInfo);
    }

    @Override
    public void onPKFinish() {
        PKManager.get().onPKFinish();
    }

    @Override
    public void onReceivePKInvitation(String inviterRoomId, String inviterUserId) {
        // ??????????????????????????????????????????????????????????????????pk??????
        if (RCDataManager.get().getMixType() == RCLiveMixType.RCMixTypeOneToOne.getValue() && getInviteStatusType() == STATUS_NOT_INVITRED) {
            UserProvider.provider().getAsyn(inviterUserId, new IResultBack<UserInfo>() {
                @Override
                public void onResult(UserInfo userInfo) {
                    PKManager.get().onReceivePKInvitation(inviterRoomId, inviterUserId);
                }
            });
        } else {
            // ?????????????????????????????????????????????pk???????????????????????????
            RCLiveEngine.getInstance().rejectPKInvitation(inviterRoomId, inviterUserId, PKResponse.busy.name(), null);
        }
    }

    @Override
    public void onPKInvitationCanceled(String inviterRoomId, String inviterUserId) {
        Logger.d(TAG, "onPKInvitationCanceled");
        PKManager.get().onPKInvitationCanceled(inviterRoomId, inviterUserId);
    }

    @Override
    public void onAcceptPKInvitationFromRoom(String inviteeRoomId, String inviteeUserId) {

    }

    @Override
    public void onRejectPKInvitationFromRoom(String inviteeRoomId, String inviteeUserId, String reason) {
        PKResponse pkResponse = PKResponse.valueOf(reason);
        PKManager.get().onPKInvitationRejected(inviteeRoomId, inviteeUserId, pkResponse);
    }

    private static class helper {
        static final LiveEventHelper INSTANCE = new LiveEventHelper();
    }

}
