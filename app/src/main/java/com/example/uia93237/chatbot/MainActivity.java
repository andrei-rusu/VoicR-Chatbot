package com.example.uia93237.chatbot;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


import ai.api.AIDataService;
import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;


public class MainActivity extends AppCompatActivity implements AIListener {

    private final static String accessToken = "f71f8955cc7a4003995542bb46f4f9e4 ";
    private final static String botName = "Anastasia";
    private final static String userName = "User";

    private EditText editText;

    private DatabaseReference ref;
    private AIService aiService;

    // boolean used to switch between the 2 pictograms in the fab button
    boolean flagFab = true;


    @Override
    protected void onCreate(Bundle savedInstanceState){


        // Set up view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        editText = findViewById(R.id.editText);
        RelativeLayout addBtn = findViewById(R.id.addBtn);

        recyclerView.setHasFixedSize(true);
        final LinearLayoutManager linearLayoutManager=new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);


        // Set up Firebase
        ref = FirebaseDatabase.getInstance().getReference();
        ref.keepSynced(true);


        // Set up Dialogflow
        final AIConfiguration config = new AIConfiguration(accessToken, AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        aiService = AIService.getService(this, config);
        aiService.setListener(this);

        final AIDataService aiDataService = new AIDataService(config);

        final AIRequest aiRequest = new AIRequest();


        // Set up send message button listener
        addBtn.setOnClickListener(view -> {
            String message = editText.getText().toString().trim();

            if (!message.equals("")) {

                Message chatMessage = new Message(message, userName);
                ref.child("chat").push().setValue(chatMessage);

                aiRequest.setQuery(message);

                new QueryTask(aiDataService, ref).execute(aiRequest);

            }
            else {
                aiService.startListening();
            }

            editText.setText("");
        });


        // Configure listener for edit text in order to change the pic
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                ImageView fab_img = findViewById(R.id.fab_img);
                Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.ic_send_white_24dp);
                Bitmap img1 = BitmapFactory.decodeResource(getResources(), R.drawable.ic_mic_white_24dp);

                if (s.toString().trim().length() != 0 && flagFab) {
                    imageViewAnimatedChange(MainActivity.this, fab_img, img);
                    flagFab = false;
                } else if (s.toString().trim().length() == 0) {
                    imageViewAnimatedChange(MainActivity.this, fab_img, img1);
                    flagFab = true;
                }
            }

            @Override public void afterTextChanged(Editable s) {

            }
        });


        /*
         Configure FirebaseRecyclerAdapter -> FirebaseUI
          */

        // Create Adapter
        FirebaseRecyclerOptions<Message> options =
                new FirebaseRecyclerOptions.Builder<Message>()
                        .setQuery(ref.child("chat"), Message.class)
                        .build();

        FirebaseRecyclerAdapter<Message, ChatViewHolder> adapter = new FirebaseRecyclerAdapter<Message, ChatViewHolder>(options){

            @Override
            protected void onBindViewHolder(@NonNull ChatViewHolder holder, int position, @NonNull Message model) {
                if(model.getMsgUser().equals(userName)){


                    holder.rightText.setText(model.getMsgText());

                    holder.rightText.setVisibility(View.VISIBLE);
                    holder.leftText.setVisibility(View.GONE);
                }
                else{
                    holder.leftText.setText(model.getMsgText());

                    holder.rightText.setVisibility(View.GONE);
                    holder.leftText.setVisibility(View.VISIBLE);
                }
            }

            @NonNull
            @Override
            public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.msglist, parent, false);

                return new ChatViewHolder(view);

            }
        };


        // Register the Adapter
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);

                int msgCount = adapter.getItemCount();
                int lastVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();

                if (lastVisiblePosition == -1 || (positionStart >= (msgCount - 1)
                        && lastVisiblePosition == (positionStart - 1))) {
                    recyclerView.scrollToPosition(positionStart);
                }
            }
        });

        recyclerView.setAdapter(adapter);

        adapter.startListening();
    }


    /*
    AIListener interface implementation
     */

    @Override
    public void onResult(AIResponse response) {

        Result result = response.getResult();

        String message = result.getResolvedQuery();
        Message chatMessageUser = new Message(message, userName);
        ref.child("chat").push().setValue(chatMessageUser);


        String reply = result.getFulfillment().getSpeech();
        Message chatMessageBot = new Message(reply, botName);
        ref.child("chat").push().setValue(chatMessageBot);

    }

    @Override
    public void onError(AIError error) {

    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }


    // Method used to change the fab button pictograms with animation
    private static void imageViewAnimatedChange(Context c,
                                               final ImageView v, final Bitmap new_image) {
        final Animation anim_out = AnimationUtils.loadAnimation(c,
                android.R.anim.fade_out);
//        anim_out.setDuration(2000);
        final Animation anim_in = AnimationUtils.loadAnimation(c,
                android.R.anim.fade_in);
//        anim_in.setDuration(2000);
        anim_out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                v.setImageBitmap(new_image);
                anim_in.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }
                });
                v.startAnimation(anim_in);
            }
        });
        v.startAnimation(anim_out);
    }

    // AsyncTask that will complete the query to the AIDataService, remembering the result in Firebase

    private static class QueryTask extends AsyncTask<AIRequest,Void,AIResponse> {

        private AIDataService dataService;
        private DatabaseReference ref;

        private QueryTask(AIDataService dataService, DatabaseReference ref) {
            this.dataService = dataService;
            this.ref = ref;
        }

        @Override
        protected AIResponse doInBackground(AIRequest... aiRequests) {

            final AIRequest req = aiRequests[0];
            try {
                return dataService.request(req);
            } catch (AIServiceException e) {}
            return null;
        }
        @Override
        protected void onPostExecute(AIResponse response) {
            if (response != null) {

                Result result = response.getResult();
                String reply = result.getFulfillment().getSpeech();
                Message chatMessage = new Message(reply, botName);
                ref.child("chat").push().setValue(chatMessage);
            }
        }
    }
}
