package com.terrydr.mirrorscopes;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.terrydr.mirrorscopes.CameraContainer.TakePictureListener;
import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import com.terrydr.resource.R;

/**
 * 启动拍照界面
 * @author yzhang
 *
 */
@SuppressLint("ResourceAsColor") public class CameraActivity extends Activity implements View.OnClickListener, 
		TakePictureListener, OnGestureListener, OnTouchListener {
	public final static String TAG = "CameraActivity";
	private CameraContainer mContainer;
	private ImageView photos_iv;
	private TextView  return_index_bt,complete_bt,ms_image_count_tv,ms_camera_tv;
	private GestureDetector detector;
	public int i_left = 0; // 记录拍摄图像个数
	private boolean deleteFile = true;   //是否删除图片文件
	private int childCount = 0;   //记录图片浏览界面传过来的数量，初始为0
	/** 记录是拖拉照片模式还是放大缩小照片模式 */
	private static final int MODE_INIT = 0;
	/** 放大缩小照片模式 */
	private static final int MODE_ZOOM = 1;
	private int mode = MODE_INIT;// 初始状态 
	/** 用于记录拖拉图片移动的坐标位置 */
	private float startDis;
	private int zoom;  //双指缩放级别
	private FilterImageView btn_thumbnail;
	private String thumbPath;
	public static boolean PRE_CUPCAKE ; 
	private ArrayList<String> recordSelectPaths;
	private final static String mSaveRootFile = "pathImages";
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ms_activity_camera);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			setTranslucentStatus(true);
		}
		setTranslucentStatus(); //设置状态栏颜色
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  //保持屏幕长亮
		recordSelectPaths = new ArrayList<String>();// 记录选中的图片
		PRE_CUPCAKE = getSDKVersionNumber() < 23 ? true : false; 
		if(!PRE_CUPCAKE){
			checkWriteExternalPermission();  //判断如果用户阻止了权限给提示窗，目前紧对android6.0以上版本有效
		}
		
		mContainer = (CameraContainer) findViewById(R.id.container);
		return_index_bt = (TextView) findViewById(R.id.return_index_bt);
		photos_iv = (ImageView) findViewById(R.id.photos_iv);
		ms_image_count_tv = (TextView) findViewById(R.id.ms_image_count_tv);
		TextPaint tp = ms_image_count_tv.getPaint();  //安体加粗
	    tp.setFakeBoldText(true);
	    ms_camera_tv = (TextView) findViewById(R.id.ms_camera_tv);
		TextPaint ms_camera_tv_tp = ms_camera_tv.getPaint();  //安体加粗
		ms_camera_tv_tp.setFakeBoldText(true);
	    
	    complete_bt = (TextView) findViewById(R.id.complete_bt);
	    btn_thumbnail=(FilterImageView)findViewById(R.id.btn_thumbnail);
	    
	    complete_bt.setOnClickListener(this);
		return_index_bt.setOnClickListener(this);
		btn_thumbnail.setOnClickListener(this);
		photos_iv.setOnClickListener(this);
		photos_iv.setOnTouchListener(this);
		mContainer.setOnTouchListener(this);
		//手势事件处理
		detector = new GestureDetector(this);

		mContainer.setRootPath(mSaveRootFile);  //设置保存图片路径
		
		Bundle bundle = getIntent().getExtras();
		if(bundle!=null){
			deleteFile = bundle.getBoolean("deleteFile");
			childCount = bundle.getInt("childCount");
			recordSelectPaths = bundle.getStringArrayList("selectPaths");
			if(recordSelectPaths == null){
				recordSelectPaths = new ArrayList<String>();// 记录选中的图片
			}
		}
		//删除图片
		if(deleteFile){
			String rootPath_left = getFolderPath(this, mSaveRootFile);
			deleteFolder(rootPath_left);
		}
		
//		int i = dip2px(30);
//		int m = px2dip(38);
//		Log.e(TAG, "i:" + i);
//		Log.e(TAG, "m:" + m);
		
	}
	
	/**
	 * 获取sdk版本号
	 * @return
	 */
	private static int getSDKVersionNumber() {  
	    int sdkVersion;  
	    try {  
	        sdkVersion = Integer.valueOf(android.os.Build.VERSION.SDK);  
	    } catch (NumberFormatException e) {  
	        sdkVersion = 0;  
	    }  
	    return sdkVersion;  
	}  
	

	/**
	 * 以下代码处理android相关权限被禁止，提醒用户打开权限
	 * 仅对android 6.0以上版本有效
	 */
	final private int REQUEST_CODE_ASK_PERMISSIONS = 123; 
    private void checkWriteExternalPermission() { 
        int hasWriteContactsPermission = ContextCompat.checkSelfPermission(CameraActivity.this, 
                Manifest.permission.CAMERA); 
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) { 
//        	Toast.makeText(CameraActivity.this, "未获得相机权限，请到设置中授权后再尝试。", Toast.LENGTH_SHORT) .show(); 
//        	finish();
//            if (!ActivityCompat.shouldShowRequestPermissionRationale(CameraActivity.this, 
//                    Manifest.permission.CAMERA)) { 
//            			showMessageOKCancel("你需要允许获取相机权限", 
//                        new DialogInterface.OnClickListener() { 
//                            @Override 
//                            public void onClick(DialogInterface dialog, int which) { 
//                                ActivityCompat.requestPermissions(CameraActivity.this, 
//                                        new String[] {Manifest.permission.CAMERA}, REQUEST_CODE_ASK_PERMISSIONS); 
//                            } 
//                        }); 
//                return; 
//            } 
            ActivityCompat.requestPermissions(CameraActivity.this, 
                    new String[] {Manifest.permission.CAMERA}, REQUEST_CODE_ASK_PERMISSIONS); 
            return; 
        } 
    } 
    
//    /**
//     * android 6.0以上版本阻止权限提示是否开启权限
//     */
//    @Override 
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) { 
//        switch (requestCode) { 
//            case REQUEST_CODE_ASK_PERMISSIONS: 
//                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) { 
//                    // Permission Granted 
//                } else { 
//                    // Permission Denied 
//                    Toast.makeText(CameraActivity.this, "PERMISSION_GRANTED Denied", Toast.LENGTH_SHORT) 
//                            .show(); 
//                } 
//                break; 
//            default: 
//                super.onRequestPermissionsResult(requestCode, permissions, grantResults); 
//        } 
//    } 
    
//    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) { 
//        new AlertDialog.Builder(CameraActivity.this) 
//                .setMessage(message) 
//                .setPositiveButton("OK", okListener) 
//                .setNegativeButton("Cancel", null) 
//                .create() 
//                .show(); 
//    } 
    
	/**
	 * 根据手机的分辨率从 dip 的单位 转成为 px(像素)
	 * 
	 * @param dipValue  dip数值
	 * @return
	 */
	private int dip2px(float dipValue) {
		final float scale = getResources().getDisplayMetrics().density;
		return (int) (dipValue * scale + 0.5f);
	}
	
	/**
	 * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
	 * @param pxValue  px数值
	 * @return
	 */
	private int px2dip(float pxValue) {
		 final float scale = getResources().getDisplayMetrics().density;
		 return (int) (pxValue / scale + 0.5f);
	}

	/**
	 * 获取文件夹路
	 * 
	 * @param type
	 *            文件夹类
	 * @param rootPath
	 *            根目录文件夹名字 为业务流水号
	 * @return
	 */
	public static String getFolderPath(Context context, String rootPath) {
		StringBuilder pathBuilder = new StringBuilder();
		// 添加应用存储路径
		pathBuilder.append(context.getExternalFilesDir(null).getAbsolutePath());
		pathBuilder.append(File.separator);
//		// 添加文件总目
//		pathBuilder.append(context.getString(R.string.Files));
//		pathBuilder.append(File.separator);
		// 添加当然文件类别的路
		pathBuilder.append(rootPath);
		return pathBuilder.toString();
	}
	
	/**
	 * 删除图片时更新缩略图
	 */
	private void updateImageThumbnail(){
		// 获取根目录下缩略图文件夹
		String folder = FileOperateUtil.getFolderPath(this,FileOperateUtil.TYPE_IMAGE, mSaveRootFile);
		// 获取图片文件大图
		List<File> imageList = FileOperateUtil.listFiles(folder, ".jpg");
		List<File> allImageList = new ArrayList<File>();
		if(imageList!=null)
			allImageList.addAll(imageList);
		FileOperateUtil.sortList(allImageList, false);
		if(allImageList.isEmpty()){
			return;
		}
		Bitmap bm = BitmapFactory.decodeFile(allImageList.get(0).getAbsolutePath());
		Bitmap thumbnail=ThumbnailUtils.extractThumbnail(bm, 320, 219);
		btn_thumbnail.setImageBitmap(thumbnail);
	}
	
	/**
	 * 点击拍照事件
	 */
	private void takePictureing(){
		if (i_left >= 9) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("已拍摄九张图片,是否创建病历?")
					.setPositiveButton("确认", new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
							complete();
							dialog.dismiss();
						}
					}).setNegativeButton("取消", new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
							dialog.dismiss();
						}
					});
			builder.create().show();
		} else {
			i_left++;
			photos_iv.setEnabled(false);
			mContainer.takePicture(this);
			//闪屏动画效果
			AlphaAnimation alphaAnimation = new AlphaAnimation(0.1f, 1.0f);  
	        alphaAnimation.setDuration(100);  
	        mContainer.startAnimation(alphaAnimation);
		}
	}

	@Override
	public void onClick(View view) {
		int currentX = (int) view.getX();
		switch (view.getId()) {
		case R.id.photos_iv:
			takePictureing();
			break;
		case R.id.complete_bt:
			complete();
			break;
		case R.id.return_index_bt:
			backPrevious();
			break;
		case R.id.btn_thumbnail: //点击下方缩略图的事件跳转
			Intent intent = new Intent(CameraActivity.this, AlbumItemAty.class);
			Bundle bundle = new Bundle();
			bundle.putInt("zoom", zoom);  
			bundle.putInt("childCount", childCount); //返回mViewPager.getChildCount()
//			Log.e(TAG, "commitrecordSelectPaths:" + recordSelectPaths);
			bundle.putStringArrayList("selectPaths", recordSelectPaths);
			bundle.putString("path", thumbPath);
			bundle.putString("root", mSaveRootFile);
			intent.putExtras(bundle);
			startActivityForResult(intent, 0);
			break;
		default:
			break;
		}
	}
	
	/**
	 * 点出“完成”事件，或者拍满九张图片触发事件
	 */
	private void complete(){
		//获取根目录下缩略图文件夹
		String folder=FileOperateUtil.getFolderPath(this, FileOperateUtil.TYPE_IMAGE, mSaveRootFile);
		//获取图片文件大图
		List<File> imageList=FileOperateUtil.listFiles(folder, ".jpg");
		
		JSONObject result_Json = new JSONObject();
		if (imageList != null) {
			if (!imageList.isEmpty()) {
				JSONArray path = new JSONArray();
				for (File p : imageList) {
					path.put(p.getAbsolutePath());
				}
				try {
					result_Json.put("pathIamges", path);
				} catch (JSONException e) {
					Log.e(TAG, e.toString());
				}
			}
		}
		Intent intent1 = new Intent();
		Bundle bundle1 = new Bundle();
		bundle1.putString("result_Json", result_Json.toString());
		intent1.putExtras(bundle1);
		this.setResult(5, intent1);
		this.finish();
	}

	/**
	 * 根据路径删除指定的目录或文件，无论存在与
	 * 
	 * @param sPath
	 *            要删除的目录或文
	 * @return 删除成功返回 true，否则返 false
	 */
	private boolean deleteFolder(String sPath) {
		boolean flag = false;
		File file = new File(sPath);
		// 判断目录或文件是否存在
		if (!file.exists()) { // 不存在返回false
			return flag;
		} else {
			// 判断是否为文
			if (file.isFile()) { // 为文件时调用删除文件方法
				return deleteFile(sPath);
			} else { // 为目录时调用删除目录方法
				return deleteDirectory(sPath);
			}
		}
	}

	/**
	 * 删除单个文件
	 * 
	 * @param sPath
	 *            被删除文件的文件
	 * @return 单个文件删除成功返回true，否则返回false
	 */
	public boolean deleteFile(String sPath) {
		boolean flag = false;
		File file = new File(sPath);
		// 路径为文件且不为空则进行删除
		if (file.isFile() && file.exists()) {
			file.delete();
			flag = true;
		}
		return flag;
	}

	/**
	 * 删除目录（文件夹）以及目录下的文
	 * 
	 * @param sPath
	 *            被删除目录的文件路径
	 * @return 目录删除成功返回true，否则返回false
	 */
	private boolean deleteDirectory(String sPath) {
		// 如果sPath不以文件分隔符结尾，自动添加文件分隔
		if (!sPath.endsWith(File.separator)) {
			sPath = sPath + File.separator;
		}
		File dirFile = new File(sPath);
		// 如果dir对应的文件不存在，或者不是一个目录，则
		if (!dirFile.exists() || !dirFile.isDirectory()) {
			return false;
		}
		boolean flag = true;
		// 删除文件夹下的所有文(包括子目)
		File[] files = dirFile.listFiles();
		for (int i = 0; i < files.length; i++) {
			// 删除子文件
			if (files[i].isFile()) {
				flag = deleteFile(files[i].getAbsolutePath());
				if (!flag)
					break;
			} // 删除子目录
			else {
				flag = deleteDirectory(files[i].getAbsolutePath());
				if (!flag)
					break;
			}
		}
		if (!flag)
			return false;
		// 删除当前目录
		if (dirFile.delete()) {
			return true;
		} else {
			return false;
		}
	}

	
	/**
	 * 拍照结束后更新缩略图
	 */
	@Override
	public void onTakePictureEnd(Bitmap bm,String imagePath,String thumbPath) {
		startAlbumAty();  //拍照完成跳转
		if(bm!=null){
			setCameraText();
			photos_iv.setEnabled(true);
			Bitmap thumbnail=ThumbnailUtils.extractThumbnail(bm, 320, 219);
			btn_thumbnail.setImageBitmap(thumbnail);
			this.thumbPath = thumbPath;
			btn_thumbnail.setVisibility(View.VISIBLE);
		}
	}
	
	/**
	 * 设置连拍结束时保存缩略图
	 * @param bm
	 * @param thumbPath
	 */
	public void setThumbnailBitmap(Bitmap bm,String thumbPath){
		photos_iv.setEnabled(true);
		if( bm == null){
			return;
		}
		Bitmap thumbnail=ThumbnailUtils.extractThumbnail(bm, 320, 219);
		btn_thumbnail.setImageBitmap(thumbnail);
		this.thumbPath = thumbPath;
		btn_thumbnail.setVisibility(View.VISIBLE);
	}
	
	/**
	 * 拍照结束后跳转到AlbumAty
	 */
	public void startAlbumAty(){
//		Intent intent = new Intent(this, AlbumAty.class);
//		Bundle bundle = new Bundle();
//		int mexposureNum = mExposureNum; // 曝光
//		bundle.putInt("mexposureNum", mexposureNum);  
//		bundle.putInt("wb_level", wb_level);  
//		bundle.putInt("zoom", zoom);  
//		intent.putExtras(bundle);
//		startActivityForResult(intent, 0);
	}

	@Override
	public void onAnimtionEnd(Bitmap bm, boolean isVideo) {

	}

	/**
	 * 为了得到传回的数据，必须在前面的Activity中（指MainActivity类）重写onActivityResult方法
	 * 
	 * requestCode 请求码，即调用startActivityForResult()传过去的 resultCode
	 * 结果码，结果码用于标识返回数据来自哪个新Activity
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (resultCode) { // resultCode为回传的标记，回传的是RESULT_OK
		case 0:
			Bundle b = data.getExtras();
			if(b!=null){
				deleteFile = b.getBoolean("deleteFile");
				recordSelectPaths = b.getStringArrayList("selectPaths");
				childCount = b.getInt("childCount");
				zoom = b.getInt("zoom");
			}
			mContainer.setZoom(zoom);
			photos_iv.setEnabled(true);
			break;
		case 5:
			Intent intent = new Intent();
			Bundle b1 = data.getExtras();
			intent.putExtras(b1);
			this.setResult(5, intent);
			this.finish();
			break;
		case 6:
			Intent intent1 = new Intent();
			Bundle b11 = data.getExtras();
			if(b11!=null){
				recordSelectPaths = b11.getStringArrayList("selectPaths");
//				Log.e(TAG, "6recordSelectPaths:" + recordSelectPaths);
			}
			intent1.putExtras(b11);
			this.setResult(6, intent1);
			this.finish();
			break;
		default:
			break;
		}
	}
	@Override
	public boolean onDown(MotionEvent e) {
//		Log.e(TAG,"onDown");
		return true;
	}

	@Override
	public void onShowPress(MotionEvent e) {
//		Log.e(TAG,"onShowPress");
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		//触摸对焦显示
		Point point=new Point((int)e.getX(), (int)e.getY());
		mContainer.setOnFocus(e,point,null);
		return true;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
//		Log.e(TAG,"onScroll");
		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {
//		Log.e(TAG,"onLongPress");
	}
	
	/**
	 * 左右滑动大于或小事件处理
	 */
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		return false;
	}
	
	/**
	 * 触摸事件处理
	 * 1. case MotionEvent.ACTION_MOVE 相机焦距的放大、缩小
	 * 2. MotionEvent.ACTION_UP 离开拍照按钮时触发长按事件 结束连拍
	 */
	@Override
	public boolean onTouch(View view, MotionEvent event) {
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			mode = MODE_INIT;
			switch (view.getId()) {
			case R.id.photos_iv:
				return false;
			}
			break;
		case MotionEvent.ACTION_UP:
			if(mode==MODE_ZOOM){
				mContainer.setPostAtTimeZoom();
			}
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			mContainer.setZoomVisibility();
			mode = MODE_ZOOM;
			/** 计算两个手指间的距离 */
			startDis = distance(event);
			break;
		case MotionEvent.ACTION_MOVE:
			if (mode == MODE_ZOOM) {
				// 只有同时触屏两个点的时候才执行
				if (event.getPointerCount() < 2)
					return true;
				float endDis = distance(event);// 结束距离
				// 每变化10f zoom变1
				int scale = (int) ((endDis - startDis) / 10f);
				if (scale >= 1 || scale <= -1) {
					zoom = mContainer.getZoom() + scale;
					// zoom不能超出范围
					if (zoom > mContainer.getMaxZoom())
						zoom = mContainer.getMaxZoom();
					if (zoom < 0)
						zoom = 0;
					mContainer.setZoom(zoom);
					// mZoomSeekBar.setProgress(zoom);
					// 将最后一次的距离设为当前距离
					startDis = endDis;
				}
			}
			break;
		default:
			break;
		}
		return this.detector.onTouchEvent(event);
	}
	
	/** 计算两个手指间的距离 */
	private float distance(MotionEvent event) {
		float dx = event.getX(1) - event.getX(0);
		float dy = event.getY(1) - event.getY(0);
		/** 使用勾股定理返回两点之间的距离 */
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

    /**
	 * 手机返回按键事件
	 */
	@Override
	public void onBackPressed() {
		backPrevious();
//		super.onBackPressed();
	}
	
	/**
	 * 返回事件,先 判断是否存在图片
	 */
	private void backPrevious(){
		// 获取根目录下缩略图文件夹
		String folder = FileOperateUtil.getFolderPath(this,FileOperateUtil.TYPE_IMAGE, mSaveRootFile);
		// 获取图片文件大图
		List<File> imageList = FileOperateUtil.listFiles(folder, ".jpg");
		if (imageList == null) {
			finish();
			return;
		} else {
			if (imageList != null) {
				if (imageList.isEmpty()) {
					finish();
					return;
				}
			}
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("是否放弃当前拍摄图片")
				.setPositiveButton("确认", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				}).setNegativeButton("取消", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		builder.create().show();
	}
	@Override
	protected void onStart() {
		super.onStart();
	}
	@Override
	protected void onRestart(){
		super.onRestart();
//		Log.e(TAG,"onRestart");
	}
	@Override
	protected void onPause(){
		super.onPause();
//		Log.e(TAG,"onPause");
	}
	@Override
	protected void onStop(){
		super.onStop();
//		Log.e(TAG,"onStop");
	}
	
	@Override
	protected void onDestroy() {		
		super.onDestroy();
	}
	@Override
	protected void onResume() {		
		// 获取根目录下缩略图文件夹
		String folder = FileOperateUtil.getFolderPath(this,FileOperateUtil.TYPE_IMAGE, mSaveRootFile);
		// 获取图片文件大图
		List<File> imageList = FileOperateUtil.listFiles(folder, ".jpg");
		if (imageList != null) {
			if (!imageList.isEmpty())
				i_left = imageList.size();
			else {
				i_left = 0;
			}
		} else {
			i_left = 0;
		}
		//设置拍照界面的照片个数 '0/9'
		setCameraText();
		if (i_left == 0 ) {
			complete_bt.setEnabled(false);
			complete_bt.setTextColor(Color.parseColor("#9D9D9D"));
			btn_thumbnail.setVisibility(View.GONE);
		}
		//更新缩略图
		updateImageThumbnail();
		super.onResume();
	}
	
	/**
	 * 设置拍照界面的照片个数 '0/6'
	 * @param trueOrFalse  
	 */
	private void setCameraText() {
		if(i_left>0){
			complete_bt.setEnabled(true);
			complete_bt.setTextColor(Color.parseColor("#ffffff"));
			ms_image_count_tv.setText(String.valueOf(i_left));
		}else{
			ms_image_count_tv.setText("");
		}
	}
	
	/**
	 * 监听系统按键 拍照
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (photos_iv.isEnabled() && event.getRepeatCount() == 0) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_ENTER:  //空格键    66
				takePictureing();
				return true;
			case KeyEvent.KEYCODE_CAMERA:  //拍照键    27
				takePictureing();
				return true;
			case KeyEvent.KEYCODE_HEADSETHOOK:   //耳机中间键
				takePictureing();
				return true;
			case KeyEvent.KEYCODE_VOLUME_DOWN:      	//音量减小键 	 25
				takePictureing();
				return true;
			case KeyEvent.KEYCODE_VOLUME_UP:      //音量增加键 	24
				takePictureing();
				return true;
			}
		}else{
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * 设置状态的颜色
	 */
	private void setTranslucentStatus(){
		SystemBarTintManager tintManager = new SystemBarTintManager(this);
		tintManager.setStatusBarTintEnabled(true);
		tintManager.setStatusBarTintResource(R.color.transparent_background);//通知栏所需颜色
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
}
