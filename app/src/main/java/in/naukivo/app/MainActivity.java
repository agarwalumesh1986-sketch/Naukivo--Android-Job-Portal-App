package in.naukivo.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Locale;

/** NauKivo offline-first Android prototype. Data is stored on this device only. */
public class MainActivity extends Activity {
    private FirebaseFirestore db;
    private final int navy = Color.rgb(24, 49, 83);
    private final int blue = Color.rgb(35, 105, 190);
    private final int pale = Color.rgb(242, 246, 251);
    private final int green = Color.rgb(28, 125, 78);
    private LinearLayout content;
    private EditText search;
    private final ArrayList<Job> jobs = new ArrayList<>();
    private final ArrayList<String> applications = new ArrayList<>();
    private SharedPreferences prefs;
    private String candidateName = "";
    private String candidatePhone = "";
    private String candidateSkills = "";

    static class Job {
        String id, title, company, city, salary, type, description;
        Job(String id, String title, String company, String city, String salary, String type, String description) {
            this.id=id; this.title=title; this.company=company; this.city=city;
            this.salary=salary; this.type=type; this.description=description;
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        db = FirebaseFirestore.getInstance();


        prefs = getSharedPreferences("naukivo_local", MODE_PRIVATE);
        loadData();
        showHome();
        loadOnlineJobs();
    }
private void loadOnlineJobs() {
    db.collection("jobs")
        .whereEqualTo("status", "active")
        .get()
        .addOnSuccessListener(result -> {
            for (QueryDocumentSnapshot doc : result) {
                String id = doc.getId();

                boolean exists = false;
                for (Job j : jobs) {
                    if (j.id.equals(id)) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    jobs.add(new Job(
                        id,
                        doc.getString("title"),
                        doc.getString("company"),
                        doc.getString("location"),
                        doc.getString("salary"),
                        doc.getString("jobType"),
                        doc.getString("description")
                    ));
                }
            }
            showHome();
        })
        .addOnFailureListener(e ->
            message("ऑनलाइन नौकरी लोड नहीं हुई: " + e.getMessage())
        );
}
    private void loadData() {
        candidateName = prefs.getString("candidate_name", "");
        candidatePhone = prefs.getString("candidate_phone", "");
        candidateSkills = prefs.getString("candidate_skills", "");
        try {
            JSONArray saved = new JSONArray(prefs.getString("jobs", "[]"));
            for (int i=0; i<saved.length(); i++) {
                JSONObject o=saved.getJSONObject(i);
                jobs.add(new Job(o.optString("id"),o.optString("title"),o.optString("company"),o.optString("city"),o.optString("salary"),o.optString("type"),o.optString("description")));
            }
            JSONArray apps = new JSONArray(prefs.getString("applications", "[]"));
            for (int i=0; i<apps.length(); i++) applications.add(apps.optString(i));
        } catch (Exception ignored) { jobs.clear(); applications.clear(); }
        if (jobs.isEmpty()) {
            jobs.add(new Job("sample1","Accountant","Shree Traders","Agra","₹15,000–₹22,000 / माह","Full time","Tally, GST और रोज़मर्रा के अकाउंट्स का काम।"));
            jobs.add(new Job("sample2","Computer Operator","Apex Services","Agra","₹10,000–₹14,000 / माह","Full time","कंप्यूटर पर डेटा एंट्री और ऑफिस का काम।"));
            jobs.add(new Job("sample3","Sales Executive","Bright Retail","Mathura","₹12,000–₹20,000 / माह","Full time","ग्राहकों से मिलना और बिक्री का काम।"));
            jobs.add(new Job("sample4","Java Developer","Tech Solutions","Noida","₹25,000–₹40,000 / माह","Full time","Java और MySQL पर डेवलपमेंट का काम।"));
            jobs.add(new Job("sample5","Visiting Accountant","Local Business","Agra","₹8,000–₹12,000 / माह","Part time","हफ्ते में कुछ दिन अकाउंट्स की देखरेख।"));
            saveJobs();
        }
    }

    private void saveJobs() {
        JSONArray a=new JSONArray();
        try { for (Job j:jobs) { JSONObject o=new JSONObject(); o.put("id",j.id); o.put("title",j.title); o.put("company",j.company); o.put("city",j.city); o.put("salary",j.salary); o.put("type",j.type); o.put("description",j.description); a.put(o); } }
        catch(Exception ignored) {}
        prefs.edit().putString("jobs",a.toString()).apply();
    }
    private void saveApplications() {
        JSONArray a=new JSONArray(); for(String id:applications) a.put(id);
        prefs.edit().putString("applications",a.toString()).apply();
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d;
    }
    private TextView text(String value,int size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private LinearLayout vertical() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout.LayoutParams lp(int w,int h) { return new LinearLayout.LayoutParams(w,h); }
    private void gap(LinearLayout box,int height) { View v=new View(this); box.addView(v,lp(1,height)); }

    private void base(String heading,String subtitle) {
        LinearLayout page=vertical(); page.setBackgroundColor(pale);
        LinearLayout top=vertical(); top.setPadding(20,18,20,16); top.setBackground(bg(navy,0));
        top.addView(text("NAUKIVO  •  JOBS NEAR YOU",12,Color.rgb(185,215,255),true));
        top.addView(text(heading,24,Color.WHITE,true));
        top.addView(text(subtitle,13,Color.rgb(225,235,248),false)); page.addView(top);
        ScrollView scroll=new ScrollView(this); content=vertical(); content.setPadding(16,16,16,24); scroll.addView(content);
        page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this); nav.setPadding(3,5,3,5); nav.setBackgroundColor(Color.WHITE);
        addNav(nav,"नौकरियाँ",()->showHome()); addNav(nav,"मेरी प्रोफाइल",()->showProfile());
        addNav(nav,"नियोक्ता",()->showEmployer()); addNav(nav,"आवेदन",()->showApplications());
        page.addView(nav); setContentView(page);
    }
    private void addNav(LinearLayout nav,String label,Runnable action) {
        Button b=new Button(this); b.setText(label); b.setTextSize(10); b.setAllCaps(false); b.setTextColor(navy);
        b.setPadding(1,4,1,4); b.setMinWidth(0); b.setBackgroundColor(Color.WHITE); b.setOnClickListener(v->action.run());
        nav.addView(b,new LinearLayout.LayoutParams(0,-2,1));
    }
    private EditText field(String hint) {
        EditText e=new EditText(this); e.setSingleLine(true); e.setTextSize(15); e.setHint(hint);
        e.setPadding(14,8,14,8); e.setBackground(bg(Color.WHITE,14));
        LinearLayout.LayoutParams p=lp(-1,52); p.bottomMargin=10; content.addView(e,p); return e;
    }
    private EditText multiField(String hint) {
        EditText e=new EditText(this); e.setTextSize(15); e.setHint(hint); e.setGravity(Gravity.TOP);
        e.setPadding(14,10,14,10); e.setMinLines(3); e.setBackground(bg(Color.WHITE,14));
        LinearLayout.LayoutParams p=lp(-1,-2); p.bottomMargin=10; content.addView(e,p); return e;
    }
    private Button actionButton(String label,Runnable action) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setTextSize(15);
        b.setBackground(bg(blue,14)); b.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams p=lp(-1,-2); p.topMargin=7; p.bottomMargin=8; content.addView(b,p); return b;
    }
    private void message(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }

    private void showHome() {
        base("अपने लिए सही नौकरी खोजें","नौकरी खोजें या अपने शहर में अवसर देखें");
        content.addView(text("शहर, कंपनी या नौकरी का नाम",14,navy,true));
        search=field("जैसे: Accountant, Agra");
        TextView heading=text("उपलब्ध नौकरियाँ  ("+jobs.size()+")",19,navy,true);
        LinearLayout.LayoutParams hp=lp(-1,-2); hp.topMargin=8; hp.bottomMargin=10; content.addView(heading,hp);
        LinearLayout list=vertical(); content.addView(list);
        Runnable render=()->{
            list.removeAllViews(); String q=search.getText().toString().toLowerCase(Locale.ROOT).trim(); int shown=0;
            for(Job j:jobs) { String all=(j.title+" "+j.company+" "+j.city+" "+j.type).toLowerCase(Locale.ROOT);
                if(!q.isEmpty()&&!all.contains(q))continue; list.addView(jobCard(j)); shown++; }
            if(shown==0)list.addView(text("कोई नौकरी नहीं मिली। दूसरा शब्द लिखकर देखें।",14,Color.DKGRAY,false));
        };
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int before,int count){render.run();} public void afterTextChanged(Editable e){}});
        render.run();
        gap(content,8); content.addView(text("यह शुरुआती ऑफलाइन संस्करण है। इस फोन पर सेव की गई पोस्ट और प्रोफाइल इसी फोन में रहेंगी।",12,Color.GRAY,false));
    }
    private View jobCard(Job j) {
        LinearLayout card=vertical(); card.setPadding(15,13,15,13); card.setBackground(bg(Color.WHITE,16)); card.setElevation(2);
        card.addView(text(j.title,18,navy,true)); card.addView(text(j.company+"  •  "+j.city,14,Color.DKGRAY,false));
        card.addView(text(j.salary,14,green,true)); card.addView(text(j.type,12,blue,true));
        Button b=new Button(this); b.setText("विवरण देखें"); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setBackground(bg(blue,12)); b.setOnClickListener(v->showJob(j)); card.addView(b);
        LinearLayout holder=vertical(); holder.addView(card); LinearLayout.LayoutParams p=lp(-1,-2); p.bottomMargin=12; holder.setLayoutParams(p); return holder;
    }
    private void showJob(Job j) {
        base("नौकरी का विवरण","आवेदन करने से पहले जानकारी पढ़ें");
        content.addView(text(j.title,23,navy,true)); gap(content,5);
        content.addView(text(j.company+"  •  "+j.city,16,Color.DKGRAY,false)); gap(content,5);
        content.addView(text(j.salary,18,green,true)); content.addView(text("काम का प्रकार: "+j.type,14,blue,true)); gap(content,12);
        content.addView(text(j.description,15,Color.DKGRAY,false));
        actionButton("इस नौकरी के लिए आवेदन करें",()->applyFor(j));
    }
    private void applyFor(Job j) {
        if(candidateName.trim().isEmpty()||candidatePhone.trim().isEmpty()) { message("पहले मेरी प्रोफाइल में नाम और मोबाइल नंबर सेव करें।"); showProfile(); return; }
        if(!applications.contains(j.id)){applications.add(j.id);saveApplications();}
        message("आपका आवेदन इस फोन में सेव हो गया है। यह नियोक्ता को ऑनलाइन नहीं भेजा गया है।"); showApplications();
    }
    private void showProfile() {
        base("उम्मीदवार प्रोफाइल","अपनी जानकारी इस फोन में सेव करें");
        content.addView(text("नाम",14,navy,true)); EditText name=field("अपना पूरा नाम"); name.setText(candidateName);
        content.addView(text("मोबाइल नंबर",14,navy,true)); EditText phone=field("10 अंकों का मोबाइल नंबर"); phone.setInputType(3); phone.setText(candidatePhone);
        content.addView(text("कौशल / अनुभव",14,navy,true)); EditText skills=multiField("जैसे: Tally, GST, Computer, Java"); skills.setText(candidateSkills);
        actionButton("प्रोफाइल सेव करें",()->{
            String n=name.getText().toString().trim(), p=phone.getText().toString().trim();
            if(n.isEmpty()||p.length()<10){message("कृपया नाम और सही मोबाइल नंबर भरें।");return;}
            candidateName=n; candidatePhone=p; candidateSkills=skills.getText().toString().trim();
            prefs.edit().putString("candidate_name",candidateName).putString("candidate_phone",candidatePhone).putString("candidate_skills",candidateSkills).apply();
            message("प्रोफाइल इस फोन में सेव हो गई।"); showProfile();
        });
        gap(content,8); content.addView(text("नोट: यह जानकारी अभी किसी सर्वर पर अपलोड नहीं होती।",12,Color.GRAY,false));
    }
    private void showEmployer() {
        base("नियोक्ता क्षेत्र","अपने व्यवसाय के लिए नौकरी पोस्ट करें");
        content.addView(text("नौकरी का पद",14,navy,true)); EditText title=field("जैसे: Accountant");
        content.addView(text("कंपनी / दुकान का नाम",14,navy,true)); EditText company=field("कंपनी का नाम");
        content.addView(text("शहर",14,navy,true)); EditText city=field("जैसे: Agra");
        content.addView(text("वेतन",14,navy,true)); EditText salary=field("जैसे: ₹12,000–₹18,000 / माह");
        content.addView(text("काम का प्रकार",14,navy,true));
        Spinner type=new Spinner(this); String[] types={"Full time","Part time","Work from home","Contract"};
        type.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,types)); content.addView(type,lp(-1,48));
        gap(content,8); content.addView(text("काम का विवरण",14,navy,true)); EditText desc=multiField("काम, योग्यता और अनुभव की जानकारी");
        actionButton("नौकरी पोस्ट सेव करें",()->{
            String t=title.getText().toString().trim(), c=company.getText().toString().trim(), cityName=city.getText().toString().trim();
            if(t.isEmpty()||c.isEmpty()||cityName.isEmpty()){message("पद, कंपनी और शहर भरना जरूरी है।");return;}
            String id="local_"+System.currentTimeMillis();
            jobs.add(0,new Job(id,t,c,cityName,salary.getText().toString().trim().isEmpty()?"वेतन बातचीत के अनुसार":salary.getText().toString().trim(),type.getSelectedItem().toString(),desc.getText().toString().trim()));
            saveJobs(); message("नौकरी इस फोन में पोस्ट हो गई। यह अभी दूसरे यूज़र को ऑनलाइन नहीं दिखेगी।"); showHome();
        });
        gap(content,8); content.addView(text("यह लोकल डेमो है। ऑनलाइन पोस्टिंग के लिए सुरक्षित सर्वर और डेटाबेस जोड़ना अगला चरण होगा।",12,Color.GRAY,false));
    }
    private void showApplications() {
        base("मेरे आवेदन","इस फोन से किए गए डेमो आवेदन");
        if(applications.isEmpty()){content.addView(text("अभी कोई आवेदन सेव नहीं है। नौकरी चुनकर आवेदन करें।",15,Color.DKGRAY,false));return;}
        for(String id:applications){ for(Job j:jobs){if(j.id.equals(id)){content.addView(jobCard(j));break;}} }
        gap(content,8); content.addView(text("आवेदन केवल इस फोन में सेव हैं; नियोक्ता को नहीं भेजे गए।",12,Color.GRAY,false));
    }
}
