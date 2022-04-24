package cn.rongcloud.rongcloud_scene_common_android;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.basis.ui.PermissionActivity;
import com.basis.utils.KToast;
import com.basis.wapper.IResultBack;

import cn.rongcloud.config.router.RouterPath;

@Route(path = RouterPath.ROUTER_MAIN)
public class MainActivity extends PermissionActivity {

    @Override
    public int setLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected String[] onCheckPermission() {
        return LAUNCHER_PERMISSIONS;
    }

    @Override
    protected void onAccept(boolean accept) {
        findViewById(R.id.iv_voice_room).setOnClickListener(v -> {
            checkAndRequestPermissions(CALL_PERMISSIONS, new IResultBack<Boolean>() {
                @Override
                public void onResult(Boolean aBoolean) {
                    if (aBoolean) {
                        ARouter.getInstance().build(RouterPath.ROUTER_LIVE_LIST).navigation();
                    } else {
                        KToast.show("请赋予必要的权限");
                    }
                }
            });
        });
    }
}