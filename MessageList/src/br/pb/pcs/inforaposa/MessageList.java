package br.pb.pcs.inforaposa;

import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.xmlpull.v1.XmlSerializer;

import br.pb.pcs.inforaposa.utils.Common;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MessageList extends Activity {
	
	private List<Message> messages;
	private HashMap<String, Bitmap> bitmaps = new HashMap<String, Bitmap>();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        final ListView lv = (ListView) findViewById(R.id.mainList);
        
        final ProgressDialog dialog = ProgressDialog.show(this, "", "Carregando feeds...", true);
		final Handler handler = new Handler() {
			public void handleMessage(android.os.Message msg) {
				dialog.dismiss();
			}
		};
		Thread checkUpdate = new Thread() {  
			public void run() {
				try{
				loadFeed();
				runOnUiThread(new Runnable() {
		            public void run() {
		            	if (messages == null){
		            		Toast msg = Toast.makeText(MessageList.this, "Verifique sua conexão com a Internet.", Toast.LENGTH_LONG);
							msg.setGravity(Gravity.CENTER, msg.getXOffset() / 2, msg.getYOffset() / 2);
							msg.show();
							MessageList.this.finish();
		            	}
		            	else{         	
		            		messages.remove(0);
			            	lv.setAdapter(new MessageItemAdapter(MessageList.this, R.layout.listitem, parseList(messages)));
			            	lv.setOnItemClickListener(new OnItemClickListener() {
			                    public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
			                    	Intent intent = new Intent(MessageList.this, Details.class);
			                		intent.putExtra("link", messages.get(position).getLink().toExternalForm());
			                		intent.putExtra("title", messages.get(position).getTitle());
			                		intent.putExtra("date", messages.get(position).getDate().toString());
			                		intent.putExtra("imgSrc", parseImg(messages.get(position).getDescription()));
			                		intent.putExtra("subtitle", parseSubtitle(messages.get(position).getDescription()));
			                    	startActivity(intent);
			                    }
	
			                });
		            	}
		            }
		        });
		        handler.sendEmptyMessage(0);
				} catch (NullPointerException m){
		    		Log.e("AndroidNews",m.getMessage(),m);	
		    	} catch (Throwable t){
		    		Log.e("AndroidNews",t.getMessage(),t);
		    	}

	        }
	    };
		checkUpdate.start();
    }

	private void loadFeed() throws UnknownHostException{
    	try{
    		Log.i("AndroidNews", "ParserType: SAX");
	    	FeedParser parser = FeedParserFactory.getParser();
	    	long start = System.currentTimeMillis();
	    	messages = parser.parse();
	    	long duration = System.currentTimeMillis() - start;
	    	Log.i("AndroidNews", "Parser duration=" + duration);
	    	String xml = writeXml();
	    	Log.i("AndroidNews", xml);
	    	int count = 0;
	    	for (Message msg : messages){
	    		if (msg.getTitle().length() < 200 && !msg.getDescription().equals("http://globoesporte.globo.com")){
	    			bitmaps.put(parseImg(msg.getDescription()), Common.getImageBitmap(parseImg(msg.getDescription())));
	    			if (count > 20)
						break;
	    			count ++;
	    		}
	    	}
    	}catch (Throwable t){
    		Log.e("AndroidNews",t.getMessage(),t);
    	}
    }
    
	private String writeXml(){
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		try {
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);
			serializer.startTag("", "messages");
			serializer.attribute("", "number", String.valueOf(messages.size()));
			for (Message msg: messages){
				serializer.startTag("", "message");
				serializer.attribute("", "date", msg.getDate());
				serializer.startTag("", "title");
				serializer.text(msg.getTitle());
				serializer.endTag("", "title");
				serializer.startTag("", "url");
				serializer.text(msg.getLink().toExternalForm());
				serializer.endTag("", "url");
				serializer.startTag("", "body");
				serializer.text(msg.getDescription());
				serializer.endTag("", "body");
				serializer.endTag("", "message");
			}
			serializer.endTag("", "messages");
			serializer.endDocument();
			return writer.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} 
	}
	
	public class MessageItemAdapter extends ArrayAdapter<Message> {
		private List<Message> messages;

		public MessageItemAdapter(Context context, int textViewResourceId, List<Message> messages) {
			super(context, textViewResourceId, messages);
			this.messages = messages;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.listitem, null);
			}

			Message message = messages.get(position);
			if (message != null) {
				TextView title = (TextView) v.findViewById(R.id.title);
				TextView date = (TextView) v.findViewById(R.id.date);
				ImageView image = (ImageView) v.findViewById(R.id.image);

				if (title != null) {
					title.setText(message.getTitle());
				}

				if(date != null) {
					date.setText(Common.parseDate(message.getDate()));
				}
				
				if(image != null) {
					if (!parseImg(message.getDescription()).equals("http://globoesporte.globo.com")){
						image.setImageBitmap(bitmaps.get(parseImg(message.getDescription())));
					}else{
						image.setImageResource(R.drawable.no_image);
					}
				}
			}
			return v;
		}
	}
	
	public String parseImg(String description){
		String aux = description.substring(description.indexOf("src='") + 5);
		String imgPath = aux.substring(0, aux.indexOf("'"));
		return imgPath;
	}
	
	public String parseSubtitle(String description){
		String imgPath = description.substring(description.indexOf("</a><br />") + 10);
		return imgPath;
	}
	
	public List<Message> parseList(List<Message> list){
		List<Message> aux = new ArrayList<Message>();
		for (int i = 0; i < 20; i++) {
			aux.add(list.get(i));
		}
		return aux;
	}
	
}