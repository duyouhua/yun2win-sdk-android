/**
 * Copyright (C) 2013-2014 EaseMob Technologies. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package y2w.ui.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import y2w.base.AppData;
import y2w.base.Urls;
import y2w.common.AsyncMultiPartGet;
import y2w.common.Config;
import y2w.manage.Users;
import y2w.model.Emoji;
import y2w.service.Back;
import y2w.ui.adapter.FragmentAdapter;
import y2w.ui.fragment.ContactFragment;
import y2w.ui.fragment.ConversationFragment;
import y2w.ui.fragment.SettingFragment;

import com.y2w.uikit.utils.StringUtil;
import com.y2w.uikit.utils.ToastUtil;
import com.yun2win.demo.R;

/**
 * Created by hejie on 2016/3/14.
 * 主界面
 */
public class MainActivity extends FragmentActivity {

	private ViewPager vp_pager;
	private GridView gv_menu;
	private RelativeLayout rl_menu_top;
	private ListView lv_menu_top;

	private List<MainMenu> menus;
	private MainMenuAdapter adapter;
	private Context context;
	private ImageButton imgbutton_search,getImgbutton_more;
	private ControlsOnClick controlsclick;
	private int unread =0;
	private int index = -1;
	Handler updatenumHandler= new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if(msg.what == 1){//更新
					int num = (int) msg.obj;
					unread =num;
					adapter.notifyDataSetChanged();
			}else if(msg.what == 2){//下载表情
				List<Emoji> emojiList = (List<Emoji>) msg.obj;
				emojiDownLoad(emojiList);
			}
		}
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		context = this;
		AppData.getInstance().setMainActivity(this);
		vp_pager = (ViewPager) findViewById(R.id.vp_stock);
		gv_menu = (GridView) findViewById(R.id.gv_main_menu);
		rl_menu_top = (RelativeLayout) findViewById(R.id.rl_menu_top);
		lv_menu_top = (ListView) findViewById(R.id.lv_menu_top);
		gv_menu.setOnItemClickListener(new MenuClick());
		lv_menu_top.setOnItemClickListener(new MenuTopClick());
		controlsclick =  new ControlsOnClick();
		rl_menu_top.setOnClickListener(controlsclick);
		context = this;
		InitViewPager();
		initMenu();

		getActionBar().hide();
        inittopMenu();

		//理约云消息通道服务器连接
		Users.getInstance().getCurrentUser().getImBridges().connect();
		Users.getInstance().getCurrentUser().getEmojis().getRemote().getEmojiList(new Back.Result<List<Emoji>>() {
			@Override
			public void onSuccess(List<Emoji> emojiList) {
				Message msg = new Message();
				msg.what = 2;
				msg.obj = emojiList;
				updatenumHandler.sendMessage(msg);
			}

			@Override
			public void onError(int code, String error) {

			}
		});
		//toast初始化
		ToastUtil.initToast(context);
	}


	private void emojiDownLoad(List<Emoji> emojiList){
		if(emojiList == null || emojiList.size() < 1)
			return;
		File file = new File(Config.CACHE_PATH_EMOJI+"base");
		if(!file.exists()){
			file.mkdirs();
		}
		for(int i = 0;i < emojiList.size();i++) {
			Emoji emoji = emojiList.get(i);
			String emojiName = emoji.getEntity().getName() + ".png";
			File file1 = new File(file.getPath() + "/" + emojiName);
			if (!file1.exists()) {
				try {
					AsyncMultiPartGet get = new AsyncMultiPartGet(Users.getInstance().getCurrentUser().getToken(), Urls.User_Messages_EMOJI_DownLoad + emoji.getEntity().getUrl(), Config.CACHE_PATH_EMOJI + "base/", emojiName);
					get.execute();
					if ((i + 1) % 5 == 0) {
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
   /*
   **更新消息数量
    */
	public void updatemessagenum(int num){
		Message msgnum = new Message();
		msgnum.what =1;
		msgnum.obj = num;
		updatenumHandler.sendMessage(msgnum);
	}
	private void inittopMenu(){
		imgbutton_search = (ImageButton) findViewById(R.id.right_search);
		imgbutton_search.setVisibility(View.GONE);
		getImgbutton_more = (ImageButton) findViewById(R.id.right_add);
		imgbutton_search.setOnClickListener(controlsclick);
		getImgbutton_more.setOnClickListener(controlsclick);
	}
	private List<Fragment> fragmentList = new ArrayList<Fragment>();
	public void InitViewPager(){
		Fragment sessionFragment = ConversationFragment.newInstance(this, context);
		Fragment contactFragment = ContactFragment.newInstance(this, context);
		Fragment settingFragment = SettingFragment.newInstance(this,context);
		fragmentList.add(sessionFragment);
		fragmentList.add(contactFragment);
		fragmentList.add(settingFragment);

		//给ViewPager设置适配器
		vp_pager.setAdapter(new FragmentAdapter(this.getSupportFragmentManager(), fragmentList));
		vp_pager.setCurrentItem(0);//设置当前显示标签页为第一页
		vp_pager.setOnPageChangeListener(new MyOnPageChangeListener());//页面变化时的监听器
		vp_pager.setOffscreenPageLimit(fragmentList.size()-1);
	}

	public class MyOnPageChangeListener implements ViewPager.OnPageChangeListener {

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {

		}

		@Override
		public void onPageScrollStateChanged(int arg0) {

		}

		@Override
		public void onPageSelected(int arg0) {
			menuIndexDisplay(arg0);
			index = arg0;
		}
	}

	/**************************************Click**************************************/
	private class MenuClick implements AdapterView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
								long arg3) {
			menuIndexDisplay(arg2);
			vp_pager.setCurrentItem(arg2);
		}

	}
	private class MenuTopClick implements AdapterView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
								long arg3) {
			if(getResources().getString(R.string.add_contact).equals(strings.get(arg2))){
				startActivity(new Intent(context,AddContactActivity.class));
				rl_menu_top.setVisibility(View.GONE);
			}else if(getResources().getString(R.string.group_start).equals(strings.get(arg2))){
				Intent intent = new Intent(context, SessionStartActivity.class);
				Bundle bundle = new Bundle();
				bundle.putBoolean("iscreate",true);
				intent.putExtras(bundle);
				startActivity(intent);
				rl_menu_top.setVisibility(View.GONE);
			}
		}

	}


	private class ControlsOnClick implements View.OnClickListener{

		@Override
		public void onClick(View v) {

           switch (v.getId()){
			   case R.id.rl_menu_top:
				   rl_menu_top.setVisibility(View.GONE);
				   break;
			   case R.id.right_search:
				   seachViewClick();
				   break;
			   case R.id.right_add:
				   menuTopDisplay();
				   break;
		   }
		}
	}
	private void seachViewClick() {
		//跳转搜索
		Intent intent = new Intent();
		intent.setClass(this,SearchActivity.class);
		intent.putExtra("index",index+"");
		startActivity(intent);
	}
	//界面下方主菜单初始化
	private void initMenu(){
		menus = new ArrayList<MainActivity.MainMenu>();
		MainMenu menu1 = new MainMenu();
		menu1.setName(getResources().getString(R.string.menu_1));
		menu1.setChecked_IconId(R.drawable.lyy_mainchecked_session);
		menu1.setNocheck_IconId(R.drawable.lyy_mainnochcke_session);
		menu1.setCurrent(true);
		MainMenu menu2 = new MainMenu();
		menu2.setName(getResources().getString(R.string.menu_2));
		menu2.setChecked_IconId(R.drawable.lyy_mainchecked_contacts);
		menu2.setNocheck_IconId(R.drawable.lyy_mainnocheck_contacts);
		menu2.setCurrent(false);
		MainMenu menu3 = new MainMenu();
		menu3.setName(getResources().getString(R.string.menu_3));
		menu3.setChecked_IconId(R.drawable.lyy_mainchecked_setting);
		menu3.setNocheck_IconId(R.drawable.lyy_mainnochcke_setting);
		menu3.setCurrent(false);

		menus.add(menu1);
		menus.add(menu2);
		menus.add(menu3);

		setMenuHorizontal();

	}

	private void setMenuHorizontal(){
		adapter = new MainMenuAdapter();
		gv_menu.setAdapter(adapter);
		int size = menus.size();
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int height = dm.widthPixels;
		int itemWidth = height / size;

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		gv_menu.setLayoutParams(params); // 设置GirdView布局参数,横向布局的关键
		gv_menu.setColumnWidth(itemWidth); // 设置列表项宽
		gv_menu.setStretchMode(GridView.NO_STRETCH);
		gv_menu.setNumColumns(size); // 设置列数量=列表集合数
	}


	private void menuIndexDisplay(int index){
		for(int i=0;i<menus.size();i++){
			if(i == index){
				menus.get(index).setCurrent(true);
               if(index==3) {
				   imgbutton_search.setVisibility(View.GONE);
				   getImgbutton_more.setVisibility(View.GONE);
			   }
			   else{
				   //imgbutton_search.setVisibility(View.VISIBLE);//搜索功能完善后，可见
				   imgbutton_search.setVisibility(View.GONE);
				   getImgbutton_more.setVisibility(View.VISIBLE);
			   }
			}else{
				menus.get(i).setCurrent(false);
			}
		}
		adapter.notifyDataSetChanged();
	}


	private void menuTopDisplay(){
		if(rl_menu_top.getVisibility() == View.VISIBLE){
			rl_menu_top.setVisibility(View.GONE);
		}else{
			strings = new ArrayList<String>();
			strings.add(getResources().getString(R.string.add_contact));
			strings.add(getResources().getString(R.string.group_start));
			rl_menu_top.setVisibility(View.VISIBLE);
			lv_menu_top.setAdapter(new MenuTopAdapter());
		}
	}

	class MainMenu{
		String name;
		int checked_iconId,nochecke_iconId;
		boolean current;

		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public int getChecked_IconId(){
			return  checked_iconId;
		}
		public void setChecked_IconId(int checked_iconId){
			this.checked_iconId = checked_iconId;
		}
		public int getNocheck_IconId(){
			return  nochecke_iconId;
		}
		public void setNocheck_IconId(int nochecke_iconId){
			this.nochecke_iconId = nochecke_iconId;
		}

		public boolean isCurrent() {
			return current;
		}
		public void setCurrent(boolean current) {
			this.current = current;
		}

	}

	class MainMenuAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return menus == null ? 0 : menus.size();
		}

		@Override
		public Object getItem(int arg0) {
			return menus.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int arg0, View arg1, ViewGroup arg2) {
			MenuViewHolder viewHolder;
			MainMenu mainMenu = menus.get(arg0);
			if(arg1 == null){
				viewHolder = new MenuViewHolder();
				arg1 = LayoutInflater.from(getApplicationContext()).inflate(R.layout.main_menu_item, null);
				viewHolder.tv_name = (TextView) arg1.findViewById(R.id.tv_menu_name);
				viewHolder.img_icon = (ImageView) arg1.findViewById(R.id.img_menu_icon);
				viewHolder.tv_count= (TextView) arg1.findViewById(R.id.tv_count);
				arg1.setTag(viewHolder);
			}else{
				viewHolder = (MenuViewHolder) arg1.getTag();
			}

			viewHolder.tv_name.setText(mainMenu.getName());
			if(mainMenu.isCurrent()){
				viewHolder.tv_name.setTextColor(Color.parseColor("#7d7d7d"));
				viewHolder.img_icon.setImageResource(mainMenu.getChecked_IconId());
			}else{
				viewHolder.tv_name.setTextColor(Color.parseColor("#7d7d7d"));
				viewHolder.img_icon.setImageResource(mainMenu.getNocheck_IconId());
			}
			if(arg0==0) {
				if (unread == 0) {
					viewHolder.tv_count.setVisibility(View.GONE);
				} else {
					viewHolder.tv_count.setVisibility(View.VISIBLE);
					viewHolder.tv_count.setText(unread + "");
				}
			}
			return arg1;
		}
	}
	class MenuViewHolder{
		TextView tv_name;
		ImageView img_icon;
		TextView tv_count;
	}
	private List<String> strings;
	class MenuTopAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			return strings == null ? 0 : strings.size();
		}

		@Override
		public Object getItem(int position) {
			return strings.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			ViewHolder viewHolder;
			if(view == null){
				viewHolder = new ViewHolder();
				view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.activity_menu_top_item, null);
				viewHolder.tv_title = (TextView) view.findViewById(R.id.tv_title);
				view.setTag(viewHolder);
			}else{
				viewHolder = (ViewHolder) view.getTag();
			}
			viewHolder.tv_title.setText(strings.get(position));
			return view;
		}

		class ViewHolder{
			TextView tv_title;
		}
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			moveTaskToBack(true);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onResume() {
		super.onResume();

	}

	@Override
	protected void onPause() {
		super.onPause();
	}

}
