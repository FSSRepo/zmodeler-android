package com.fastsmartsystem.saf;
import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.webkit.*;
import com.forcex.utils.*;
import android.content.*;
import com.forcex.net.*;
import com.forcex.app.threading.*;
import com.forcex.*;
import java.io.*;
import android.graphics.*;

public class ZMStoreActivity extends Activity {
	WebView web;
	ImageView ivResult;
	LinearLayout layError;
	TextView tvTitle,tvDescription;
	Button btnOk;
	String title;
	ProgressBar webLoading;
	boolean no_error = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.store);
		web = findViewById(R.id.webviewStore);
		web.setWebViewClient(new ClientHandler());
		web.setWebChromeClient(new ChromeHandler());
		web.getSettings().setJavaScriptEnabled(true);
		web.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		layError = findViewById(R.id.error_layout);
		ivResult = findViewById(R.id.ivResult);
		tvTitle = findViewById(R.id.tvTitle);
		tvDescription = findViewById(R.id.tvDescription);
		webLoading = findViewById(R.id.pbLoad);
		btnOk = findViewById(R.id.btnOk);
		File dir = new File(FX.fs.homeDirectory+"zmdl/cache");
		if(!dir.exists()){
			dir.mkdir();
		}
		web.loadUrl("http://fssrepo.github.io/testpage.github.io/");
		web.setVisibility(View.GONE);
		Toast.makeText(this,Zmdl.gt("please_wait"),Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onBackPressed() {
		if(no_error && web.canGoBack()){
			web.goBack();
		}else{
			finish();
		}
	}
	
	int tries = 0;
	
	public void showError(String info){
		web.setVisibility(View.GONE);
		webLoading.setVisibility(View.GONE);
		layError.setVisibility(View.VISIBLE);
		tvTitle.setText(Zmdl.gt(info+"t"));
		tvDescription.setText(Zmdl.gt(info+"i"));
		if(info.equals("no_internet")){
			ivResult.setImageResource(R.drawable.no_network);
			if(tries < 3){
				btnOk.setText(Zmdl.gt("try_again"));
			}else{
				btnOk.setText(Zmdl.gt("exit"));
			}
			btnOk.setOnClickListener(new View.OnClickListener(){
					@Override
					public void onClick(View p1) {
						if(tries >= 3){
							finish();
						}else{
							layError.setVisibility(View.GONE);
							webLoading.setVisibility(View.VISIBLE);
							no_error = true;
							web.loadUrl("http://fssrepo.github.io/testpage.github.io/");
							Toast.makeText(ZMStoreActivity.this,Zmdl.gt("please_wait"),Toast.LENGTH_SHORT).show();
							tries++;
						}
					}
				});
		}else if(info.equals("download_error")){
			ivResult.setImageResource(R.drawable.download_error);
			btnOk.setText(Zmdl.gt("accept"));
			btnOk.setOnClickListener(new View.OnClickListener(){
					@Override
					public void onClick(View p1) {
						layError.setVisibility(View.GONE);
						if(no_error){
							web.setVisibility(View.VISIBLE);
						}else{
							showError("no_internet");
						}
					}
				});
		}
	}
	
	private class ChromeHandler extends WebChromeClient {
		@Override
		public void onReceivedTitle(WebView view, String title) {
			super.onReceivedTitle(view, title);
			ZMStoreActivity.this.title = title;
		}
	}
	
	private class ClientHandler extends WebViewClient {
		
		@Override
		public WebResourceResponse shouldInterceptRequest(WebView view,final String url) {
			if(url.endsWith(".zpk")){
				runOnUiThread(new Runnable(){
						@Override
						public void run() {
							Zmdl.adtsk(new DownloadHandler(url,title));
						}
				});
			}
			return super.shouldInterceptRequest(view, url);
		}
		
		
		@Override
		public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            String des = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                des = error.getDescription().toString();
            }
            if(des.contains("ERR_NAME_NOT_RESOLVED") || des.contains("ERR_ADDRESS_UNREACHABLE")){
				showError("no_internet");
				no_error = false;
			}
		}

		@Override
		public void onPageFinished(WebView view, String url)
		{
			// TODO:  this method
			super.onPageFinished(view, url);
			if(no_error){
				web.setVisibility(View.VISIBLE);
				webLoading.setVisibility(view.GONE);
			}
		}
	}
	
	private class DownloadHandler implements Task,OnDownloadListener
	{
		Download download;
		boolean cancel;
		
		@Override
		public boolean execute() {
			int offset = 0;
			for(offset = url.length() - 1;offset >= 0;offset--){
				if(url.charAt(offset) == '/'){
					break;
				}
			}
			String pack = url.substring(offset,url.length());
			download = new Download(url,FX.fs.homeDirectory+"zmdl/cache/"+pack,this);
			if(download.process()){
				runOnUiThread(new Runnable(){
						@Override
						public void run() {
							Toast.makeText(ZMStoreActivity.this,Zmdl.gt("finished"),Toast.LENGTH_SHORT).show();
							dialog.dismiss();
						}
					});
			}else if(cancel){
				runOnUiThread(new Runnable(){
						@Override
						public void run() {
							Toast.makeText(ZMStoreActivity.this,Zmdl.gt("canceled"),Toast.LENGTH_SHORT).show();
						}
					});
			}else{
				runOnUiThread(new Runnable(){
						@Override
						public void run() {
							dialog.dismiss();
						}
					});
			}
			return true;
		}
		
		AlertDialog dialog;
		int total_size = 0;
		TextView tvInfo;
		ProgressBar pbar;
		String url;
		
		public DownloadHandler(String url,String name){
			dialog = new AlertDialog.Builder(ZMStoreActivity.this).create();
			View container = LayoutInflater.from(ZMStoreActivity.this).inflate(R.layout.download,null);
			dialog.setTitle(Zmdl.gt("downloading")+" "+name);
			pbar = container.findViewById(R.id.pbdownload);
			pbar.setMax(100);
			tvInfo = container.findViewById(R.id.tv_download);
			tvInfo.setText(Zmdl.gt("wait_server"));
			dialog.setButton(Zmdl.gt("cancel"), new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface p1, int p2) {
						cancel = true;
						if(download != null){
							download.cancel();
						}
						Toast.makeText(ZMStoreActivity.this,Zmdl.gt("canceling"),Toast.LENGTH_SHORT).show();
					}
			});
			dialog.setView(container);
			dialog.setCancelable(false);
			dialog.show();
			this.url = url;
		}
		
		@Override
		public void requestFileSize(int size) {
			total_size = size;
		}

		@Override
		public void onDownloadError(int error_code, String url,final String details) {
			runOnUiThread(new Runnable(){
					@Override
					public void run() {
						showError("download_error");
						Logger.log(details);
					}
				});
		}

		@Override
		public void onDownloadProgress(final long size_dowloaded) {
			runOnUiThread(new Runnable(){
					@Override
					public void run() {
						float percent = ((float)size_dowloaded / total_size) * 100f;
						tvInfo.setText(String.format("%.2f",percent)+"%");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            pbar.setProgress((int)percent,true);
                        }
                    }
			});
		}
	}
}
