package io.github.nickid2018.atribot.backend.onebot;

import cn.evole.onebot.sdk.action.ActionPath;

public class ReactionActionPath implements ActionPath {

    public static final ReactionActionPath INSTANCE = new ReactionActionPath();

    private ReactionActionPath() {
    }

    @Override
    public String getPath() {
        return "set_msg_emoji_like";
    }
}
