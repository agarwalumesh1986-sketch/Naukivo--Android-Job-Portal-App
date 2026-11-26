
package in.naukivo.app;

import android.app.Activity;
import android.app.AlertDialog;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final int navy = Color.rgb(24, 49, 83);
    private final int blue = Color.rgb(35, 105, 190);
    private final int pale = Color.rgb(242, 246, 251);
    private final int green = Color.rgb(28, 125, 78);

    private LinearLayout content;
    private EditText search;
    private SharedPreferences prefs;

    private final ArrayList<Job> jobs = new ArrayList<>();
    private final ArrayList<String> applications = new ArrayList<>();

    private String candidateName = "";
    private String candidatePhone = "";
    private String candidateSkills = "";

    static class Job {
        String id, title, company, city, salary, type, description, employerUid;

        Job(String id, String title, String company, String city,
            String salary, String type, String description) {
            this.id = id;
            this.title = title;
            this.company = company;
            this.city = city;
            this.salary = salary;
            this.type = type;
            this.description = description;
            this.employerUid = "";
        }
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        prefs = getSharedPreferences("naukivo_local", MODE_PRIVATE);

        loadData();
        showHome();

        // Firebase anonymous login
        auth.signInAnonymously()
            .addOnSuccessListener(result -> {
                message("Firebase से कनेक्ट हो गया।");
                loadOnlineJobs();
            })
            .addOnFailureListener(e ->
                message("Firebase Login Error: " + e.getMessage())
            );
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
                        Job onlineJob = new Job(
                            id,
                            safe(doc.getString("title")),
                            safe(doc.getString("company")),
                            safe(doc.getString("location")),
                            safe(doc.getString("salary")),
                            doc.contains("jobType") ? safe(doc.getString("jobType")) : safe(doc.getString("job_type")),
                            safe(doc.getString("description"))
                        );
                        onlineJob.employerUid = safe(doc.getString("employerUid"));
                        jobs.add(onlineJob);
                    }
                }
                showHome();
            })
            .addOnFailureListener(e ->
                message("ऑनलाइन नौकरी लोड नहीं हुई: " + e.getMessage())
            );
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void loadData() {
        candidateName = prefs.getString("candidate_name", "");
        candidatePhone = prefs.getString("candidate_phone", "");
        candidateSkills = prefs.getString("candidate_skills", "");

        try {
            JSONArray saved = new JSONArray(
                prefs.getString("jobs", "[]")
            );

            for (int i = 0; i < saved.length(); i++) {
                JSONObject o = saved.getJSONObject(i);
                Job savedJob = new Job(
                    o.optString("id"),
                    o.optString("title"),
                    o.optString("company"),
                    o.optString("city"),
                    o.optString("salary"),
                    o.optString("type"),
                    o.optString("description")
                );
                savedJob.employerUid = o.optString("employerUid", "");
                jobs.add(savedJob);
            }

            JSONArray apps = new JSONArray(
                prefs.getString("applications", "[]")
            );

            for (int i = 0; i < apps.length(); i++) {
                applications.add(apps.optString(i));
            }

        } catch (Exception ignored) {
            jobs.clear();
            applications.clear();
        }

        if (jobs.isEmpty()) {
            jobs.add(new Job("sample1", "Accountant",
                "Shree Traders", "Agra", "₹15,000–₹22,000 / माह",
                "Full time", "Tally, GST और अकाउंट्स का काम।"));

            jobs.add(new Job("sample2", "Computer Operator",
                "Apex Services", "Agra", "₹10,000–₹14,000 / माह",
                "Full time", "कंप्यूटर और डेटा एंट्री का काम।"));

            jobs.add(new Job("sample3", "Sales Executive",
                "Bright Retail", "Mathura", "₹12,000–₹20,000 / माह",
                "Full time", "बिक्री और ग्राहक सेवा का काम।"));

            jobs.add(new Job("sample4", "Java Developer",
                "Tech Solutions", "Noida", "₹25,000–₹40,000 / माह",
                "Full time", "Java और MySQL का काम।"));

            jobs.add(new Job("sample5", "Visiting Accountant",
                "Local Business", "Agra", "₹8,000–₹12,000 / माह",
                "Part time", "हफ्ते में कुछ दिन अकाउंट्स का काम।"));

            saveJobs();
        }
    }

    private void saveJobs() {
        JSONArray a = new JSONArray();

        try {
            for (Job j : jobs) {
                JSONObject o = new JSONObject();
                o.put("id", j.id);
                o.put("title", j.title);
                o.put("company", j.company);
                o.put("city", j.city);
                o.put("salary", j.salary);
                o.put("type", j.type);
                o.put("description", j.description);
                o.put("employerUid", j.employerUid);
                a.put(o);
            }
        } catch (Exception ignored) {}

        prefs.edit().putString("jobs", a.toString()).apply();
    }

    private void saveApplications() {
        JSONArray a = new JSONArray();
        for (String id : applications) a.put(id);

        prefs.edit().putString("applications", a.toString()).apply();
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);

        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    private void gap(LinearLayout box, int height) {
        View v = new View(this);
        box.addView(v, lp(1, height));
    }

    private void base(String heading, String subtitle) {
        LinearLayout page = vertical();
        page.setBackgroundColor(pale);

        LinearLayout top = vertical();
        top.setPadding(20, 18, 20, 16);
        top.setBackground(bg(navy, 0));

        top.addView(text("NAUKIVO • JOBS NEAR YOU",
            12, Color.rgb(185, 215, 255), true));
        top.addView(text(heading, 24, Color.WHITE, true));
        top.addView(text(subtitle, 13, Color.rgb(225, 235, 248), false));

        page.addView(top);

        ScrollView scroll = new ScrollView(this);
        content = vertical();
        content.setPadding(16, 16, 16, 24);
        scroll.addView(content);

        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setPadding(3, 5, 3, 5);
        nav.setBackgroundColor(Color.WHITE);

        addNav(nav, "नौकरियाँ", () -> showHome());
        addNav(nav, "मेरी प्रोफाइल", () -> showProfile());
        addNav(nav, "नियोक्ता", () -> showEmployer());
        addNav(nav, "आवेदन", () -> showApplications());

        page.addView(nav);
        setContentView(page);
    }

    private void addNav(LinearLayout nav, String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(10);
        b.setAllCaps(false);
        b.setTextColor(navy);
        b.setPadding(1, 4, 1, 4);
        b.setMinWidth(0);
        b.setBackgroundColor(Color.WHITE);
        b.setOnClickListener(v -> action.run());

        nav.addView(b, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setTextSize(15);
        e.setHint(hint);
        e.setPadding(14, 8, 14, 8);
        e.setBackground(bg(Color.WHITE, 14));

        LinearLayout.LayoutParams p = lp(-1, 52);
        p.bottomMargin = 10;
        content.addView(e, p);
        return e;
    }

    private EditText multiField(String hint) {
        EditText e = new EditText(this);
        e.setTextSize(15);
        e.setHint(hint);
        e.setGravity(Gravity.TOP);
        e.setPadding(14, 10, 14, 10);
        e.setMinLines(3);
        e.setBackground(bg(Color.WHITE, 14));

        LinearLayout.LayoutParams p = lp(-1, -2);
        p.bottomMargin = 10;
        content.addView(e, p);
        return e;
    }

    private Button actionButton(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setBackground(bg(blue, 14));
        b.setOnClickListener(v -> action.run());

        LinearLayout.LayoutParams p = lp(-1, -2);
        p.topMargin = 7;
        p.bottomMargin = 8;
        content.addView(b, p);
        return b;
    }

    private void message(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private void showHome() {
        base("अपने लिए सही नौकरी खोजें",
            "नौकरी खोजें या अपने शहर में अवसर देखें");

        content.addView(text("शहर, कंपनी या नौकरी का नाम",
            14, navy, true));

        search = field("जैसे: Accountant, Agra");

        TextView heading = text(
            "उपलब्ध नौकरियाँ (" + jobs.size() + ")",
            19, navy, true
        );

        LinearLayout.LayoutParams hp = lp(-1, -2);
        hp.topMargin = 8;
        hp.bottomMargin = 10;
        content.addView(heading, hp);

        LinearLayout list = vertical();
        content.addView(list);

        Runnable render = () -> {
            list.removeAllViews();

            String q = search.getText().toString()
                .toLowerCase(Locale.ROOT).trim();

            int shown = 0;

            for (Job j : jobs) {
                String all = (j.title + " " + j.company + " "
                    + j.city + " " + j.type).toLowerCase(Locale.ROOT);

                if (!q.isEmpty() && !all.contains(q)) continue;

                list.addView(jobCard(j));
                shown++;
            }

            if (shown == 0) {
                list.addView(text(
                    "कोई नौकरी नहीं मिली। दूसरा शब्द लिखकर देखें।",
                    14, Color.DKGRAY, false
                ));
            }
        };

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) {
                render.run();
            }
            public void afterTextChanged(Editable e) {}
        });

        render.run();

        gap(content, 8);
        content.addView(text(
            "नौकरियाँ Firebase से ऑनलाइन लोड हो सकती हैं।",
            12, Color.GRAY, false
        ));
    }

    private View jobCard(Job j) {
        LinearLayout card = vertical();
        card.setPadding(15, 13, 15, 13);
        card.setBackground(bg(Color.WHITE, 16));
        card.setElevation(2);

        card.addView(text(j.title, 18, navy, true));
        card.addView(text(j.company + " • " + j.city,
            14, Color.DKGRAY, false));
        card.addView(text(j.salary, 14, green, true));
        card.addView(text(j.type, 12, blue, true));

        Button b = new Button(this);
        b.setText("विवरण देखें");
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackground(bg(blue, 12));
        b.setOnClickListener(v -> showJob(j));
        card.addView(b);

        LinearLayout holder = vertical();
        holder.addView(card);

        LinearLayout.LayoutParams p = lp(-1, -2);
        p.bottomMargin = 12;
        holder.setLayoutParams(p);

        return holder;
    }

    private void showJob(Job j) {
        base("नौकरी का विवरण", "आवेदन करने से पहले जानकारी पढ़ें");

        content.addView(text(j.title, 23, navy, true));
        gap(content, 5);

        content.addView(text(j.company + " • " + j.city,
            16, Color.DKGRAY, false));

        gap(content, 5);
        content.addView(text(j.salary, 18, green, true));
        content.addView(text("काम का प्रकार: " + j.type,
            14, blue, true));

        gap(content, 12);
        content.addView(text(j.description, 15, Color.DKGRAY, false));

        actionButton("इस नौकरी के लिए आवेदन करें", () -> applyFor(j));
    }

    private void applyFor(Job j) {
        if (j.id.startsWith("sample")) {
            message("यह डेमो नौकरी है। ऑनलाइन नौकरी चुनकर आवेदन करें।");
            return;
        }

        if (candidateName.trim().isEmpty()
            || candidatePhone.trim().length() < 10) {
            message("पहले प्रोफाइल में नाम और मोबाइल नंबर सेव करें।");
            showProfile();
            return;
        }

        if (auth.getCurrentUser() == null) {
            message("Firebase Login नहीं हुआ। इंटरनेट चेक करें।");
            return;
        }

        if (applications.contains(j.id)) {
            message("आप इस नौकरी के लिए पहले आवेदन कर चुके हैं।");
            showApplications();
            return;
        }

        CheckBox sharePhone = new CheckBox(this);
        sharePhone.setText("मेरा मोबाइल नंबर नियोक्ता को दें");
        sharePhone.setChecked(false);

        new AlertDialog.Builder(this)
            .setTitle("आवेदन भेजने की पुष्टि")
            .setMessage("आपका नाम और कौशल नौकरी पोस्ट करने वाले नियोक्ता को भेजे जाएंगे। मोबाइल नंबर वैकल्पिक है।")
            .setView(sharePhone)
            .setNegativeButton("रद्द करें", (dialog, which) -> dialog.dismiss())
            .setPositiveButton("आवेदन भेजें", (dialog, which) ->
                submitApplication(j, sharePhone.isChecked()))
            .show();
    }

    private void submitApplication(Job j, boolean sharePhone) {
        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("jobId", j.id);
        data.put("jobTitle", j.title);
        data.put("company", j.company);
        data.put("employerUid", j.employerUid);
        data.put("applicantUid", uid);
        data.put("applicantName", candidateName);
        data.put("applicantSkills", candidateSkills);
        data.put("phoneShared", sharePhone);
        if (sharePhone) data.put("applicantPhone", candidatePhone);
        data.put("createdAt", System.currentTimeMillis());

        db.collection("applications")
            .add(data)
            .addOnSuccessListener(ref -> {
                applications.add(j.id);
                saveApplications();

                message("आपका आवेदन ऑनलाइन भेज दिया गया!");
                showApplications();
            })
            .addOnFailureListener(e ->
                message("आवेदन नहीं भेजा गया: " + e.getMessage())
            );
    }

    private void showProfile() {
        base("उम्मीदवार प्रोफाइल", "अपनी जानकारी इस फोन में सेव करें");

        content.addView(text("नाम", 14, navy, true));
        EditText name = field("अपना पूरा नाम");
        name.setText(candidateName);

        content.addView(text("मोबाइल नंबर", 14, navy, true));
        EditText phone = field("10 अंकों का मोबाइल नंबर");
        phone.setInputType(3);
        phone.setText(candidatePhone);

        content.addView(text("कौशल / अनुभव", 14, navy, true));
        EditText skills = multiField("जैसे: Tally, GST, Computer, Java");
        skills.setText(candidateSkills);

        actionButton("प्रोफाइल सेव करें", () -> {
            String n = name.getText().toString().trim();
            String p = phone.getText().toString().trim();

            if (n.isEmpty() || p.length() < 10) {
                message("कृपया नाम और सही मोबाइल नंबर भरें।");
                return;
            }

            candidateName = n;
            candidatePhone = p;
            candidateSkills = skills.getText().toString().trim();

            prefs.edit()
                .putString("candidate_name", candidateName)
                .putString("candidate_phone", candidatePhone)
                .putString("candidate_skills", candidateSkills)
                .apply();

            message("प्रोफाइल इस फोन में सेव हो गई।");
            showProfile();
        });

        gap(content, 8);
        content.addView(text(
            "मोबाइल नंबर ऐप में लोकल सेव रहता है। आवेदन में मोबाइल नंबर अपलोड नहीं होता।",
            12, Color.GRAY, false
        ));
    }

    private void showEmployer() {
        base("नियोक्ता क्षेत्र", "अपने व्यवसाय के लिए नौकरी पोस्ट करें");

        actionButton("मेरे नौकरी आवेदक देखें", () -> showEmployerApplications());

        content.addView(text("नौकरी का पद", 14, navy, true));
        EditText title = field("जैसे: Accountant");

        content.addView(text("कंपनी / दुकान का नाम", 14, navy, true));
        EditText company = field("कंपनी का नाम");

        content.addView(text("शहर", 14, navy, true));
        EditText city = field("जैसे: Agra");

        content.addView(text("वेतन", 14, navy, true));
        EditText salary = field("जैसे: ₹12,000–₹18,000 / माह");

        content.addView(text("काम का प्रकार", 14, navy, true));

        Spinner type = new Spinner(this);
        String[] types = {
            "Full time", "Part time",
            "Work from home", "Contract"
        };

        type.setAdapter(new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_dropdown_item, types
        ));

        content.addView(type, lp(-1, 48));
        gap(content, 8);

        content.addView(text("काम का विवरण", 14, navy, true));
        EditText desc = multiField("काम, योग्यता और अनुभव की जानकारी");

        actionButton("नौकरी ऑनलाइन पोस्ट करें", () -> {
            String t = title.getText().toString().trim();
            String c = company.getText().toString().trim();
            String cityName = city.getText().toString().trim();

            if (t.isEmpty() || c.isEmpty() || cityName.isEmpty()) {
                message("पद, कंपनी और शहर भरना जरूरी है।");
                return;
            }

            if (auth.getCurrentUser() == null) {
                message("Firebase Login नहीं हुआ। इंटरनेट चेक करें।");
                return;
            }

            String id = "job_" + System.currentTimeMillis();
            String uid = auth.getCurrentUser().getUid();

            String pay = salary.getText().toString().trim();
            if (pay.isEmpty()) pay = "वेतन बातचीत के अनुसार";

            final String finalPay = pay;

            String jobType = type.getSelectedItem().toString();
            String description = desc.getText().toString().trim();

            Map<String, Object> data = new HashMap<>();
            data.put("title", t);
            data.put("company", c);
            data.put("location", cityName);
            data.put("salary", finalPay);
            data.put("jobType", jobType);
            data.put("description", description);
            data.put("status", "active");
            data.put("employerUid", uid);
            data.put("createdAt", System.currentTimeMillis());

            db.collection("jobs")
                .document(id)
                .set(data)
                .addOnSuccessListener(unused -> {
                    jobs.add(0, new Job(
                        id, t, c, cityName, finalPay, jobType, description
                    ));

                    saveJobs();
                    message("नौकरी ऑनलाइन पोस्ट हो गई!");
                    showHome();
                })
                .addOnFailureListener(e ->
                    message("नौकरी पोस्ट नहीं हुई: " + e.getMessage())
                );
        });

        gap(content, 8);
        content.addView(text(
            "पोस्ट करने के लिए Firebase Login और इंटरनेट जरूरी है।",
            12, Color.GRAY, false
        ));
    }

    private void showEmployerApplications() {
        base("मेरे नौकरी आवेदक", "आपकी पोस्ट की गई नौकरियों पर आए आवेदन");

        if (auth.getCurrentUser() == null) {
            content.addView(text("Firebase Login नहीं हुआ। इंटरनेट चेक करें।", 14, Color.DKGRAY, false));
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        content.addView(text("आवेदक सूची लोड हो रही है...", 14, Color.GRAY, false));

        db.collection("applications")
            .whereEqualTo("employerUid", uid)
            .get()
            .addOnSuccessListener(result -> {
                content.removeAllViews();

                if (result.isEmpty()) {
                    content.addView(text("अभी आपकी नौकरी पर कोई आवेदन नहीं आया है।", 15, Color.DKGRAY, false));
                    return;
                }

                for (QueryDocumentSnapshot doc : result) {
                    LinearLayout card = vertical();
                    card.setPadding(14, 12, 14, 12);
                    card.setBackground(bg(Color.WHITE, 14));

                    card.addView(text(safe(doc.getString("jobTitle")), 17, navy, true));
                    card.addView(text(safe(doc.getString("company")), 13, Color.DKGRAY, false));
                    card.addView(text("आवेदक: " + safe(doc.getString("applicantName")), 15, navy, true));
                    card.addView(text("कौशल: " + safe(doc.getString("applicantSkills")), 13, Color.DKGRAY, false));

                    if (Boolean.TRUE.equals(doc.getBoolean("phoneShared"))) {
                        card.addView(text("मोबाइल: " + safe(doc.getString("applicantPhone")), 14, green, true));
                    } else {
                        card.addView(text("मोबाइल नंबर साझा नहीं किया गया।", 12, Color.GRAY, false));
                    }

                    LinearLayout.LayoutParams cp = lp(-1, -2);
                    cp.bottomMargin = 12;
                    content.addView(card, cp);
                }
            })
            .addOnFailureListener(e -> {
                content.removeAllViews();
                content.addView(text("आवेदक सूची नहीं खुल सकी। Firebase Rules में employerUid के आधार पर applications पढ़ने की अनुमति जरूरी है।", 14, Color.DKGRAY, false));
            });
    }

    private void showApplications() {
        base("मेरे आवेदन", "इस फोन से किए गए आवेदन");

        if (applications.isEmpty()) {
            content.addView(text(
                "अभी कोई आवेदन सेव नहीं है। नौकरी चुनकर आवेदन करें।",
                15, Color.DKGRAY, false
            ));
            return;
        }

        for (String id : applications) {
            for (Job j : jobs) {
                if (j.id.equals(id)) {
                    content.addView(jobCard(j));
                    break;
                }
            }
        }

        gap(content, 8);
        content.addView(text(
            "आवेदन Firebase में ऑनलाइन भेजे जाते हैं।",
            12, Color.GRAY, false
        ));
    }
}
