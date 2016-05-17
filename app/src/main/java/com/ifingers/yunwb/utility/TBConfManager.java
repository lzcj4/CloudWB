package com.ifingers.yunwb.utility;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.tb.conf.api.struct.ant.CAntThumbnail;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import tb.confui.module.DocShareModule;
import tb.confui.module.ITBConfKitListener;
import tb.confui.module.TBConfKit;

public class TBConfManager
{
	public static final String	TAG	= "test";

	/**
	 * 加会
	 * 
	 * @param context
	 * @param confExportListener
	 * @param appKey
	 */
	public static void joinConf(Context context, ITBConfKitListener confExportListener, DisplayMetrics displayMetrics, String appKey, String cmdLine,
								String cmdLine_init )
	{

		// 初始化密钥、站点
		TBConfKit.getInstance().init( context, appKey, cmdLine_init, displayMetrics );
		// 加会的回调监听
		TBConfKit.getInstance().setTBConfKitListener( confExportListener );
		//加会
		TBConfKit.getInstance().joinConf( context, cmdLine );

	}

	/**
	 * 创会
	 * 
	 * @param context
	 * @param confExportListener
	 * @param appKey
	 */
	public static void createConf(Context context, ITBConfKitListener confExportListener, DisplayMetrics displayMetrics, String appKey, String cmdLine,
								  String cmdLine_init )
	{

		// 初始化密钥、站点
		TBConfKit.getInstance().init( context, appKey, cmdLine_init, displayMetrics );
		// 加会的回调监听
		TBConfKit.getInstance().setTBConfKitListener( confExportListener );
		//加会
		TBConfKit.getInstance().createConf(context, cmdLine);

	}

	public static void leaveConf(boolean isHost) {
		TBConfKit.getInstance().leaveconf(isHost);
	}

	public static void dispose() {
		TBConfKit.getInstance().unInit();
	}

	public static void mute(boolean isMute) {
		TBConfKit.getInstance().audioModule().muteIntputDevice(isMute);
	}

	public static ArrayList<CAntThumbnail> getDocList() {
		return TBConfKit.getInstance().DocShareModule().getDocList();
	}

	public static void showDoc(FrameLayout parent, int docId) {
		DocShareModule docModule = TBConfKit.getInstance().DocShareModule();
		docModule.showDocShare(parent);
		docModule.setDoc(docId);
	}

	public static void hideDoc(){
		DocShareModule docShareModule = TBConfKit.getInstance().DocShareModule();
		docShareModule.hideDocShare();
	}

	public static String toJsonForConfCmdLineInit( String node_sitename )
	{
		// 初始化一个JSON字符串,用来存放站点信息
		String cmdLine_init = "";
		try
		{
			// 构建一个JSONObject对象
			JSONObject jsonObject = new JSONObject();
			// 键值对，存放站点信息
			jsonObject.put( TBConfKit.NODE_SITENAME, node_sitename );
			// 生成字符串
			cmdLine_init = jsonObject.toString();
		}
		// 捕获异常
		catch ( JSONException e )
		{
			e.printStackTrace();
			return "";
		}
		return cmdLine_init;
	}

	public static String toJsonForJoinConfCmdLine( String node_meetingid, String node_meetingpwd, String node_displayname, String node_username,
			String node_uilayout, String node_meetinghostpwd, String node_meetingtopic, String node_createconfdisplayname )
	{
		// 初始化一个JSON字符串,用来存放加会信息
		String cmdLine = "";
		try
		{
			// 构建一个JSONObject对象
			JSONObject jsonObject = new JSONObject();
			// 键值对，存放会议ID
			jsonObject.put( TBConfKit.NODE_MEETINGID, node_meetingid );
			// 键值对，存会议密码
			jsonObject.put( TBConfKit.NODE_MEETINGPWD, node_meetingpwd );
			// 键值对，存放用户的显示名
			jsonObject.put( TBConfKit.NODE_DISPLAYNAME, node_displayname );
			// 键值对，存放用户名(通常为帐号)
			jsonObject.put( TBConfKit.NODE_USERNAME, node_username );
			// 键值对，存放UI布局(没有额外说明，默认填 1)
			jsonObject.put( TBConfKit.NODE_UILAYOUT, node_uilayout );
			/******************* 选填参数 *****************/
			// 键值对，存放用户的主持人密码
			jsonObject.put( TBConfKit.NODE_MEETINGHOSTPWD, node_meetinghostpwd );
			// 键值对，存放会议主题
			jsonObject.put( TBConfKit.NODE_MEETINGTOPIC, node_meetingtopic );
			// 创建者显示名
			jsonObject.put( TBConfKit.NODE_CREATECONFDISPLAYNAME, node_createconfdisplayname );

			// 生成字符串
			cmdLine = jsonObject.toString();
		}
		// 捕获异常
		catch ( JSONException e )
		{
			e.printStackTrace();
			return "";
		}
		return cmdLine;
	}

	public static String toJsonForCreateConfCmdLine( String node_meetingpwd, String node_displayname, String node_username, String node_uilayout,
			String node_meetinghostpwd, String node_meetingtopic, boolean node_immediatelyjoinconf )
	{
		// 初始化一个JSON字符串,用来存放创会信息
		String cmdLine = "";
		try
		{
			// 构建一个JSONObject对象
			JSONObject jsonObject = new JSONObject();
			// 键值对，存放会议密码
			jsonObject.put( TBConfKit.NODE_MEETINGPWD, node_meetingpwd );
			// 键值对，存放用户的显示名
			jsonObject.put( TBConfKit.NODE_DISPLAYNAME, node_displayname );
			// 键值对，存放用户的用户名
			jsonObject.put( TBConfKit.NODE_USERNAME, node_username );
			// 键值对，存放UI布局(没有额外说明，默认填 1)
			jsonObject.put( TBConfKit.NODE_UILAYOUT, node_uilayout );
			/******************* 选填参数 *****************/
			// 键值对，存放会议主题
			jsonObject.put( TBConfKit.NODE_MEETINGTOPIC, node_meetingtopic );
			// 键值对，存放用户的主持人密码
			jsonObject.put( TBConfKit.NODE_MEETINGHOSTPWD, node_meetinghostpwd );
			// 创建会议后，是否立即加入会议
			jsonObject.put( TBConfKit.NODE_IMMEDIATELYJOINCONF, node_immediatelyjoinconf );

			// 生成字符串
			cmdLine = jsonObject.toString();
		}
		// 捕获异常
		catch ( JSONException e )
		{
			e.printStackTrace();
			return "";
		}
		return cmdLine;
	}

}
