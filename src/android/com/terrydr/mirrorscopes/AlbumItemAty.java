package com.terrydr.mirrorscopes;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.terrydr.mirrorscopes.MatrixImageView.OnSingleTapListener;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.terrydr.resource.R;

/** 
 * @ClassName: AlbumItemAty 
 * @Description:相册图片大图Activity 包含图片编辑功能
 * @date 20160419
 *  
 */
public class AlbumItemAty extends Activity implements OnClickListener,OnSingleTapListener{
	public final static String TAG="AlbumDetailAty";
	private String mSaveRoot;
	private AlbumViewPager mViewPager;//显示大图
	private TextView mBackView,header_bar_photo_commit_bt;
	private TextView mCountView,selected_tv,eye_left_select_count_tv;
	private View mHeaderBar,mBottomBar;
	private ImageView header_bar_photo_back_iv;
	private ViewGroup group;
	private CheckBox albumitem_selected_cb;
	private String[] albumitem_selected_cb_bool;  
	private List<String> paths;
	private List<File> files;
	private ArrayList<String> selectPaths = new ArrayList<String>();// 选中的图片
	private ArrayList<String> selectPathsLeft = new ArrayList<String>();// 选中的图片  左眼图片
	private boolean leftOrRight = true;
	private Bundle bundle;
	private boolean isPlugin = false;  //标记是否是plugin传过来的,默认为false:否;ture:是
	private ArrayList<String> recordSelectPaths;// 选中的图片
	// 装点点的ImageView数组 
    private ImageView[] tips;  
    private SharedPreferences preferences;   //保存勾选要提交的图片路径
    private SharedPreferences.Editor editor;
    private static final String SAVEFILENAME = "saveSelectPaths";
    private static final String SAVEKEYNAME = "sKey";
    //记录activity,CameraActivity:true;AlbumItemAty:false
    private static final String ISCAMERAACTIVITY = "isCameraActivity";  
    private final static String mROOTLEFT = "pathImages";  //保存图片的根目录
    private int childCount = 0;   
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ms_albumitem);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			setTranslucentStatus(true);
		}
		setTranslucentStatus(); //设置状态栏颜色
		recordSelectPaths = new ArrayList<String>();// 选中的图片
		bundle = getIntent().getExtras();
		if (bundle != null) {
			isPlugin = bundle.getBoolean("isPlugin");
			childCount = bundle.getInt("childCount");
			if(!isPlugin){  //如果不是js端传过来的直接获取上个activiy传参
				recordSelectPaths = bundle.getStringArrayList("selectPaths");
			}else{
				Set<String> keySet = getSharedPreferences(SAVEKEYNAME);
				if(keySet != null){
					recordSelectPaths = new ArrayList<String>(keySet);
				}
			}
//			Log.e(TAG, "recordSelectPaths:" + recordSelectPaths);
//			Log.e(TAG, "recordSelectPaths.size:" + recordSelectPaths.size());
		}
		group = (ViewGroup)findViewById(R.id.imagegroup_ll); 
		mViewPager=(AlbumViewPager)findViewById(R.id.albumviewpager);
		mBackView=(TextView)findViewById(R.id.header_bar_photo_back);
		header_bar_photo_back_iv=(ImageView)findViewById(R.id.header_bar_photo_back_iv);
		mCountView=(TextView)findViewById(R.id.header_bar_photo_count);
		mHeaderBar=findViewById(R.id.album_item_header_bar);
		mBottomBar=findViewById(R.id.album_item_bottom_bar);
		albumitem_selected_cb = (CheckBox)findViewById(R.id.albumitem_selected_cb);
		header_bar_photo_commit_bt = (TextView) findViewById(R.id.header_bar_photo_commit_bt);
		selected_tv= (TextView)findViewById(R.id.selected_tv);
		eye_left_select_count_tv = (TextView)findViewById(R.id.eye_left_select_count_tv);
//		eye_left_select_count_tv.setText("0/9");
		
		TextPaint tp = mCountView.getPaint();  //字体加粗
	    tp.setFakeBoldText(true);

	    header_bar_photo_commit_bt.setOnClickListener(this);
	    albumitem_selected_cb.setOnClickListener(this);
		mBackView.setOnClickListener(this);
		header_bar_photo_back_iv.setOnClickListener(this);

		mSaveRoot=getIntent().getExtras().getString("root");
		mViewPager.setOnPageChangeListener(pageChangeListener);
		String currentFileName=null;
		if(getIntent().getExtras()!=null)
			currentFileName=getIntent().getExtras().getString("path");
		if(currentFileName!=null){
			File file=new File(currentFileName);
			currentFileName=file.getName();
			if(currentFileName.indexOf(".")>0)
				currentFileName=currentFileName.substring(0, currentFileName.lastIndexOf("."));
		}
		loadAlbum(mSaveRoot,currentFileName);
		
//		mViewPager.setOnSlideUpListener(AlbumItemAty.this);
	}
	
	/**
	 * 设置状态的颜色
	 */
	private void setTranslucentStatus(){
		SystemBarTintManager tintManager = new SystemBarTintManager(this);
		tintManager.setStatusBarTintEnabled(true);
		tintManager.setStatusBarTintResource(R.color.common_title_bg);//通知栏所需颜色
	}
	
	@TargetApi(19) 
	private void setTranslucentStatus(boolean on) {
		Window win = getWindow();
		WindowManager.LayoutParams winParams = win.getAttributes();
		final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
		if (on) {
			winParams.flags |= bits;
		} else {
			winParams.flags &= ~bits;
		}
		win.setAttributes(winParams);
	}
	
	/**  
	 *  加载图片
	 *  @param rootPath   图片根路
	 */
	public void loadAlbum(String rootPath,String fileName){
		//获取根目录下缩略图文件夹
		String folder=FileOperateUtil.getFolderPath(this, FileOperateUtil.TYPE_IMAGE, mROOTLEFT);
		//获取图片文件大图
		List<File> imageList=FileOperateUtil.listFiles(folder, ".jpg");
		
		files=new ArrayList<File>();
		if(imageList!=null&&imageList.size()>0){
			files.addAll(imageList);
			eye_left_select_count_tv.setVisibility(View.VISIBLE);
		} else {
			eye_left_select_count_tv.setVisibility(View.GONE);
		}
		FileOperateUtil.sortList(files, true);
        //将点点加入到ViewGroup中  
        tips = new ImageView[files.size()];  
        albumitem_selected_cb_bool = new String[files.size()];
		
		if(files.size()>0){
			paths=new ArrayList<String>();
			int currentItem=0;
			int i = 0;
			for (File file : files) {
				if(fileName!=null && file.getName().contains(fileName))
					currentItem=files.indexOf(file);
				paths.add(file.getAbsolutePath());

				ImageView imageView = new ImageView(this);
				imageView.setLayoutParams(new LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				tips[i] = imageView;
				Log.e(TAG, "childCount:" + childCount);
				Log.e(TAG, "files:" + files.size());
				Log.e(TAG, "i:" + i);
				if(isPlugin){
					if(recordSelectPaths != null){
						if(recordSelectPaths.contains(file.getAbsolutePath())){
							albumitem_selected_cb_bool[i] = "true";
							tips[i].setBackgroundResource(R.drawable.albumitem_selected_status);
							albumitem_selected_cb.setChecked(true);
							selectPaths.add(file.getAbsolutePath());
							if (file.getAbsolutePath().contains(mROOTLEFT)) {
								selectPathsLeft.add(file.getAbsolutePath());
							} 
						}else{
							albumitem_selected_cb_bool[i] = "false";
							tips[i].setBackgroundResource(R.drawable.albumitem_unselect_status);
							albumitem_selected_cb.setChecked(false);
							selectPaths.remove(file.getAbsolutePath());
							if (file.getAbsolutePath().contains(mROOTLEFT)) {
								selectPathsLeft.remove(file.getAbsolutePath());
							} 
						}
					}
				}else
				if(childCount > 0 && i < childCount){
					if(recordSelectPaths != null){
						if(recordSelectPaths.contains(file.getAbsolutePath())){
							albumitem_selected_cb_bool[i] = "true";
							tips[i].setBackgroundResource(R.drawable.albumitem_selected_status);
							albumitem_selected_cb.setChecked(true);
							selectPaths.add(file.getAbsolutePath());
							if (file.getAbsolutePath().contains(mROOTLEFT)) {
								selectPathsLeft.add(file.getAbsolutePath());
							} 
						}else{
							albumitem_selected_cb_bool[i] = "false";
							tips[i].setBackgroundResource(R.drawable.albumitem_unselect_status);
							albumitem_selected_cb.setChecked(false);
							selectPaths.remove(file.getAbsolutePath());
							if (file.getAbsolutePath().contains(mROOTLEFT)) {
								selectPathsLeft.remove(file.getAbsolutePath());
							} 
						}
					}
				}else{
					albumitem_selected_cb_bool[i] = "true";
					tips[i].setBackgroundResource(R.drawable.albumitem_selected_status);
					albumitem_selected_cb.setChecked(true);
					selectPaths.add(file.getAbsolutePath());
					if (file.getAbsolutePath().contains(mROOTLEFT)) {
						selectPathsLeft.add(file.getAbsolutePath());
					} 
				}
				LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
						new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				layoutParams.leftMargin = 10;
				layoutParams.rightMargin = 10;
				group.addView(imageView, layoutParams);
				i++;
			}
			mViewPager.setAdapter(mViewPager.new ViewPagerAdapter(this,paths));
//			mViewPager.setCurrentItem(currentItem);
//			tips[currentItem].setBackgroundResource(R.drawable.albumitem_selected_current);
			tips[0].setBackgroundResource(R.drawable.albumitem_selected_current);
			mViewPager.setCurrentItem(0);
			if (albumitem_selected_cb_bool[0].equals("true")) {
				albumitem_selected_cb.setChecked(true);
			} else {
				albumitem_selected_cb.setChecked(false);
			}
//			setCountView();
			eye_left_select_count_tv.setText(selectPathsLeft.size()+ "/" + files.size());
			if(selectPathsLeft.size()>0){
				header_bar_photo_commit_bt.setEnabled(true);
				header_bar_photo_commit_bt.setTextColor(Color.parseColor("#ffffff"));
			}else{
				header_bar_photo_commit_bt.setEnabled(false);
				header_bar_photo_commit_bt.setTextColor(Color.parseColor("#9D9D9D"));
			}
		}
	}

	
	/**
	 * 删除照片重新加载图片，以及修改一些状态
	 * @param paths
	 * @param deletePath
	 * @param selectPath
	 * @param deleteCurretItem
	 * @param getCurrentItem
	 */
	public void reloadAlbum(List<String> paths, String deletePath,String selectPath,int deleteCurretItem,int getCurrentItem) {
		if(paths.isEmpty()){
			backPrevious();
			return;
		}
		
		// 获取根目录下缩略图文件夹
		String folder = FileOperateUtil.getFolderPath(this,FileOperateUtil.TYPE_IMAGE, mROOTLEFT);
		// 获取图片文件大图
		List<File> imageList = FileOperateUtil.listFiles(folder, ".jpg");

		files = new ArrayList<File>();
		if (imageList != null && imageList.size() > 0) {
			files.addAll(imageList);
			eye_left_select_count_tv.setVisibility(View.VISIBLE);
		} else {
			eye_left_select_count_tv.setVisibility(View.GONE);
		}
		FileOperateUtil.sortList(files, true);
		// 将点点加入到ViewGroup中
		tips = new ImageView[files.size()];
		List<String> new_albumitem_selected_cb_bool = new ArrayList<String>();
		for (int i = 0; i < albumitem_selected_cb_bool.length; i++) {
			if (i != deleteCurretItem) {
				new_albumitem_selected_cb_bool.add(albumitem_selected_cb_bool[i]);
			}
		}
		albumitem_selected_cb_bool = new String[new_albumitem_selected_cb_bool.size()];
		if (files.size() > 0) {
			group.removeAllViews();
			paths = new ArrayList<String>();
			int i = 0;
			for (File file : files) {
				if (selectPath != null && file.getName().contains(selectPath))
				paths.add(file.getAbsolutePath());

				ImageView imageView = new ImageView(this);
				imageView.setLayoutParams(new LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				tips[i] = imageView;
				albumitem_selected_cb_bool[i] = String.valueOf(new_albumitem_selected_cb_bool.toArray()[i]);
				if(albumitem_selected_cb_bool[i].equals("true")){
					tips[i].setBackgroundResource(R.drawable.albumitem_selected_status);
				}else
					tips[i].setBackgroundResource(R.drawable.albumitem_unselect_status);
				LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
						new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT,
								LayoutParams.WRAP_CONTENT));
				layoutParams.leftMargin = 10;
				layoutParams.rightMargin = 10;
				group.addView(imageView, layoutParams);
				i++;
			}
			tips[getCurrentItem].setBackgroundResource(R.drawable.albumitem_selected_current);
			
			setImageBackground(getCurrentItem);
			setCheckBoxSelected(getCurrentItem);
			if (deletePath.contains(mROOTLEFT)) {
				selectPathsLeft.remove(deletePath);
			} 
			eye_left_select_count_tv.setText(selectPathsLeft.size()+ "/" + files.size());
			if(selectPathsLeft.size()>0){
				header_bar_photo_commit_bt.setEnabled(true);
				header_bar_photo_commit_bt.setTextColor(Color.parseColor("#ffffff"));
			}else{
				header_bar_photo_commit_bt.setEnabled(false);
				header_bar_photo_commit_bt.setTextColor(Color.parseColor("#9D9D9D"));
			}
		}
	}
	
	/** 
     * 设置标题imageview的状态 
     * @param selectItems 选中当前图片的索引
     */  
	private void setImageBackground(int selectItems) {
		for (int i = 0; i < tips.length; i++) {
			if (i == selectItems) {
				tips[i].setBackgroundResource(R.drawable.albumitem_selected_current);
			} else {
				if (albumitem_selected_cb_bool[i].equals("true")) {
					tips[i].setBackgroundResource(R.drawable.albumitem_selected_status);
				} else {
					tips[i].setBackgroundResource(R.drawable.albumitem_unselect_status);
				}

			}
		}
	}
    
	/** 
     * 设置选中的tip的背景 
     * @param selectItems   选中当前图片的索引
     */  
    private void setCheckBoxSelected(int selectItems){  
        for(int i=0; i<albumitem_selected_cb_bool.length; i++){  
            if(i == selectItems && albumitem_selected_cb_bool[i].equals("true")){  
            	albumitem_selected_cb.setChecked(true);
            	break;
            }else{  
            	albumitem_selected_cb.setChecked(false); 
            }  
        }  
    } 
	
    /**
     * 图片左右滑动改变事件,理处相关事件状态
     */
	private OnPageChangeListener pageChangeListener=new OnPageChangeListener() {
		@Override
		public void onPageSelected(int position) {
//			setCountView();
			if(tips[position]!=null){
				setImageBackground(position);  //设置选中状态
				setCheckBoxSelected(position);  // 设置checkbox是否选中 
			}
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
		}
	};

	@Override
	public void onSingleTap() {
		if(mHeaderBar.getVisibility()==View.VISIBLE){
			AlphaAnimation animation=new AlphaAnimation(1, 0);
			animation.setDuration(300);
			mHeaderBar.startAnimation(animation);
			mBottomBar.startAnimation(animation);
			mHeaderBar.setVisibility(View.GONE);
			mBottomBar.setVisibility(View.GONE);
		}else {
			AlphaAnimation animation=new AlphaAnimation(0, 1);
			animation.setDuration(300);
			mHeaderBar.startAnimation(animation);
			mBottomBar.startAnimation(animation);
			mHeaderBar.setVisibility(View.VISIBLE);
			mBottomBar.setVisibility(View.VISIBLE);
		}	
	}

	/**
	 * 返回上一个activity
	 */
	private void backPrevious(){
		Intent intent = new Intent(AlbumItemAty.this, CameraActivity.class);
		if (bundle != null) {
			bundle.putBoolean("deleteFile", false);  //是否删除图片
			bundle.putStringArrayList("selectPaths", selectPaths);    //选中的图片路径
			bundle.putInt("childCount", mViewPager.getAdapter().getCount()); //当前mViewPager的节点个数
			intent.putExtras(bundle);
		}
		if (isPlugin) { //js端跳转过来返回6
			this.setResult(6, intent);
		} else {     //正常 返回0
			this.setResult(0, intent);
		}
		this.finish();
	}
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.header_bar_photo_back:
			backPrevious();
			break;
		case R.id.header_bar_photo_back_iv:
			backPrevious();
			break; 
		case R.id.albumitem_selected_cb:
			if (selectPathsLeft.size() >= 9 &&leftOrRight && albumitem_selected_cb.isChecked()) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("最多选择九 张图片").setPositiveButton("确定",
						new android.content.DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								albumitem_selected_cb.setChecked(false);
							}
						});
				builder.create().show();
			} else {
				if (mViewPager.getAdapter() != null) {
					String filePath = mViewPager.getSelectPath();
					int index = 0;
					for (File file : files) {
						if (filePath != null && filePath.contains(file.getName())) {
							index = files.indexOf(file);
							if (albumitem_selected_cb.isChecked()) {
								albumitem_selected_cb_bool[index] = "true";
								selectPaths.add(filePath);
								if (filePath.contains(mROOTLEFT)) {
									selectPathsLeft.add(filePath);
								}
							} else {
								albumitem_selected_cb_bool[index] = "false";
								selectPaths.remove(filePath);
								if (filePath.contains(mROOTLEFT)) {
									selectPathsLeft.remove(filePath);
								} 
							}
						}
					}
					eye_left_select_count_tv.setText(selectPathsLeft.size()+ "/" + files.size());
					if(selectPathsLeft.size()>0){
						header_bar_photo_commit_bt.setEnabled(true);
						header_bar_photo_commit_bt.setTextColor(Color.parseColor("#ffffff"));
					}else{
						header_bar_photo_commit_bt.setEnabled(false);
						header_bar_photo_commit_bt.setTextColor(Color.parseColor("#9D9D9D"));
					}
				}
			}
			break;
		case R.id.header_bar_photo_commit_bt:
			commitOnClick();
			break;
		default:
			break;
		}
	}
	
	/**
	 * 提交事件,如果没有选中任何图片则弹出提示信息窗
	 */
	private void commitOnClick(){
		JSONObject result_Json = new JSONObject();
		if (selectPathsLeft != null) {
			if (!selectPathsLeft.isEmpty()) {
				JSONArray path = new JSONArray();
				for (String p : selectPathsLeft) {
					path.put(p);
				}
				try {
					result_Json.put("pathIamges", path);
				} catch (JSONException e) {
					Log.e(TAG, e.toString());
				}
			}
		}
		if(result_Json.length()==0){
			Toast.makeText(getApplicationContext(), "请选择图片再提交!",Toast.LENGTH_SHORT).show();
			return;
		}
		Set<String> selectPathsSet = new HashSet<String>(selectPaths);
		saveSharedPreferences(SAVEKEYNAME,selectPathsSet); //保存选中图片数据到本地
		Intent intent1 = new Intent();
		Bundle bundle1 = new Bundle();
		bundle1.putString("result_Json", result_Json.toString());
		intent1.putExtras(bundle1);
		this.setResult(5, intent1);
		this.finish();
	}
	@Override
	public void onBackPressed() {
		backPrevious();
		super.onBackPressed();
	}
	@Override
	protected void onStop() {
		super.onStop();
	}
	
	/**
	 * 保存选中的图片路径到本地文件
	 * @param keyName     key名称
	 * @param keyValue    对应key的值 
	 */
	private void saveSharedPreferences(String keyName,Set<String> keyValue){
		//实例化SharedPreferences对象（第一步）
		preferences = getSharedPreferences(SAVEFILENAME,Activity.MODE_PRIVATE);
		//实例化SharedPreferences.Editor对象（第二步）
		editor = preferences.edit();
		//用putString的方法保存数据
		editor.putStringSet(keyName, keyValue);  
		editor.putBoolean(ISCAMERAACTIVITY, false);  //保存记住的activity状态
		//提交当前数据
		editor.commit(); 
	}
	
	/**
	 * 读取本地文件数据
	 * @param keyName key名称
	 */
	private Set<String> getSharedPreferences(String keyName) {
		preferences = getSharedPreferences(SAVEFILENAME, Activity.MODE_PRIVATE);
		// 使用getString方法获得value，注意第2个参数是value的默认值
		Set<String> keyValue = preferences.getStringSet(keyName, null);
		return keyValue;
	}

	public void onChangeTesChanged(String _text) {
		selected_tv.setText(_text);
	}
	
}
