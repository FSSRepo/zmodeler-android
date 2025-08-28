package com.fastsmartsystem.saf.store;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import com.fastsmartsystem.saf.R;
import com.fastsmartsystem.saf.Zmdl;
import com.forcex.*;
import com.forcex.app.threading.*;
import com.forcex.net.*;
import java.io.*;

public class ZMStoreActivity extends Activity {
	WebView web;
	ImageView ivResult;
	LinearLayout layError;
	TextView tvTitle,tvDescription;
	Button btnOk;
	String title;
	ProgressBar webLoading;
	boolean no_error = true;
	String page_url = "http://fssrepo.github.io/zmstore/";
	
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
		File dir = new File("zmdl/cache");
		if(!dir.exists()){
			dir.mkdir();
		}
		web.loadUrl(page_url);
		web.setVisibility(View.GONE);
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
							web.loadUrl(page_url);
							tries++;
						}
					}
				});
		}else if(info.equals("download_error")) {
			ivResult.setImageResource(R.drawable.download_error);
			btnOk.setText(Zmdl.gt("accept"));
			btnOk.setOnClickListener(v -> {
				layError.setVisibility(View.GONE);
				if(no_error){
					web.setVisibility(View.VISIBLE);
				}else{
					showError("no_internet");
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

	private class ClientHandler extends WebViewClient
	{

		@Override
		public boolean shouldOverrideUrlLoading(WebView view,final WebResourceRequest request)
		{
			if(request.getUrl().toString().endsWith(".zpk")){
				runOnUiThread(() -> {
					Toast.makeText(ZMStoreActivity.this,Zmdl.gt("please_wait"),Toast.LENGTH_SHORT).show();
					Zmdl.adtsk(new DownloadHandler(request.getUrl().toString(),title));
				});
			}
			return super.shouldOverrideUrlLoading(view,request);
		}

		@Override
		public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
			String des = null;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
				des = error.getDescription().toString();
			}
			if(des.contains("ERR_NAME_NOT_RESOLVED") || des.contains("ERR_CLEARTEXT_NOT_PERMITTED") || des.contains("ERR_ADDRESS_UNREACHABLE")){
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
				webLoading.setVisibility(View.GONE);
			}
		}
	}

	private class DownloadHandler implements Task,OnDownloadListener
	{
		Download download;
		boolean cancel;

		@Override
		public boolean execute() {
			int offset;

			for(offset = url.length() - 1;offset >= 0;offset--){
				if(url.charAt(offset) == '/') {
					break;
				}
			}
			String pack = url.substring(offset,url.length());
			download = new Download(url,"zmdl/cache/"+pack,this);
			if(download.process()) {
				runOnUiThread(() -> {
					Toast.makeText(ZMStoreActivity.this,Zmdl.gt("finished"),Toast.LENGTH_SHORT).show();
					dialog.dismiss();
				});
			} else if(cancel) {
				runOnUiThread(() -> Toast.makeText(ZMStoreActivity.this,Zmdl.gt("canceled"),Toast.LENGTH_SHORT).show());
			} else {
				runOnUiThread(() -> dialog.dismiss());
			}
			return true;
		}

		AlertDialog dialog;
		int total_size = 0;
		TextView tvInfo;
		ProgressBar progress_bar;
		String url;

		public DownloadHandler(String url,String name){
			dialog = new AlertDialog.Builder(ZMStoreActivity.this).create();
			View container = LayoutInflater.from(ZMStoreActivity.this).inflate(R.layout.download,null);
			dialog.setTitle(Zmdl.gt("downloading")+" "+name);
			progress_bar = container.findViewById(R.id.pbdownload);
			progress_bar.setMax(100);
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
					}
				});
		}

		@Override
		public void onDownloadProgress(final long size_dowloaded) {
			runOnUiThread(new Runnable() {
					@Override
					public void run() {
						float porcent = ((float)size_dowloaded / total_size) * 100f;
						tvInfo.setText(String.format("%.2f",porcent)+"%");
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
							progress_bar.setProgress((int)porcent,true);
						}
					}
				});
		}
	}
}
