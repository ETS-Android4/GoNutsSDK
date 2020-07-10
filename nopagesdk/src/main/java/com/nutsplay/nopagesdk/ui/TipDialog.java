package com.nutsplay.nopagesdk.ui;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nutsplay.nopagesdk.kernel.SDKLangConfig;
import com.nutsplay.nopagesdk.kernel.SDKManager;
import com.nutsplay.nopagesdk.utils.SDKResUtils;

/**
 * Created by frankma on 2019-10-09 18:22
 * Email: frankma9103@gmail.com
 * Desc:
 */
public class TipDialog extends Dialog {

    public TipDialog(@NonNull Context context) {
        super(context);
    }

    public TipDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    protected TipDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    public static class Builder {
        private Context context;
        private String title,content;


        public Builder(Context context, String title,String content) {
            this.context = context;
            this.title = title;
            this.content = content;
        }

        public TipDialog create() {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final TipDialog dialog = new TipDialog(context);
            if (inflater == null) return dialog;

            View layout;
            if (SDKManager.getInstance().isCommonVersion()){
                layout = inflater.inflate(SDKResUtils.getResId(context, "sdk_dialog_tips_normal", "layout"), null);
            }else {
                layout = inflater.inflate(SDKResUtils.getResId(context, "sdk_dialog_tips", "layout"), null);
            }

            TextView tvTips = layout.findViewById(SDKResUtils.getResId(context, "tv_tips", "id"));
            TextView tvContent = layout.findViewById(SDKResUtils.getResId(context, "tv_content", "id"));
            TextView enterGame = layout.findViewById(SDKResUtils.getResId(context, "tv_enter_game", "id"));

            tvTips.setText(SDKLangConfig.getInstance().findMessage("tourist_signin_tips"));
            enterGame.setText(SDKLangConfig.getInstance().findMessage("viewstring_enter_game"));

            //内容
//            tvContent.setText(SDKLangConfig.getInstance().findMessage("tourist_signin_alert"));
            tvContent.setText(content);

            //进入游戏按钮
            enterGame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            dialog.setContentView(layout);
            if (dialog.getWindow()!=null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.setCancelable(false);
            return dialog;
        }
    }

}