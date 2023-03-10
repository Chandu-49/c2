package com.example.c.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.example.c.adaptor.RecentConversationAdapter;
import com.example.c.databinding.ActivityMainBinding;
import com.example.c.listeners.ConversionListener;
import com.example.c.models.ChatMessage;
import com.example.c.models.User;
import com.example.c.utilites.Constants;
import com.example.c.utilites.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BaseActivity implements ConversionListener {
    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private List<ChatMessage>conversations;
    private FirebaseFirestore database;
    private RecentConversationAdapter conversationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager=new PreferenceManager(getApplicationContext());
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loadUserDetails();
        getToken();
        setListners();
        init();
        listenConversations();



    }
    private void init(){
        conversations=new ArrayList<>();
        conversationAdapter=new RecentConversationAdapter(conversations,this);
        binding.ConversationRecyclerView.setAdapter(conversationAdapter);
        database=FirebaseFirestore.getInstance();
    }
    private void setListners(){
        binding.imageSignOut.setOnClickListener(v -> signOut()

        );
        binding.fabNewChat.setOnClickListener(v ->
            startActivity(new Intent(getApplicationContext(),UsersActivity.class)));

        System.out.println("Byeee");
    }
    private void loadUserDetails(){
        binding.txtName.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE),Base64.DEFAULT);
        Bitmap bitmap= BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);

    }
    private void showToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void listenConversations(){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER))
                .addSnapshotListener(eventListener);
    }
    private final EventListener<QuerySnapshot>eventListener=(value, error) -> {
        if(error!=null){
            return;
        }
        if(value!=null){
            for(DocumentChange documentChange :value.getDocumentChanges()){
                if(documentChange.getType()==DocumentChange.Type.ADDED){
                    String senderId=documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverId=documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    ChatMessage chatMessage=new ChatMessage();
                    chatMessage.senderId=senderId;
                    chatMessage.receiverId=receiverId;
                    if(preferenceManager.getString(Constants.KEY_USER).equals(senderId)){
                        chatMessage.conversionImage=documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                        chatMessage.conversionName=documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                        chatMessage.conversionId=documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);

                    }
                    else{
                        chatMessage.conversionImage=documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                        chatMessage.conversionName=documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                        chatMessage.conversionId=documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    }
                    chatMessage.message=documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                    chatMessage.dateObject=documentChange.getDocument().getDate(Constants.KEY_TIME_STAMP);
                    conversations.add(chatMessage);
                }
                else if(documentChange.getType()== DocumentChange.Type.MODIFIED){
                    for(int i=0;i<conversations.size();i++){
                        String senderId=documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        String receiverId=documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        if(conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)){
                            conversations.get(i).message=documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                            conversations.get(i).dateObject=documentChange.getDocument().getDate(Constants.KEY_TIME_STAMP);
                            break;
                        }
                    }

                }
            }
            Collections.sort(conversations,(obj1,obj2)->obj2.dateObject.compareTo(obj1.dateObject));
            conversationAdapter.notifyDataSetChanged();
            binding.ConversationRecyclerView.smoothScrollToPosition(0);

            binding.ConversationRecyclerView.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);

        }
    };
    private void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }
    private  void updateToken(String token){
        preferenceManager.putString(Constants.KEY_FCM_TOKEN,token);

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference= database.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER));
        documentReference.update(Constants.KEY_FCM_TOKEN,token)
                .addOnFailureListener(e->showToast("Unable to Update Token"));

    }
    private void signOut(){
        showToast("Signing Out ....");
        FirebaseFirestore database =FirebaseFirestore.getInstance();
        DocumentReference documentReference=database.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER));
        HashMap<String,Object>updates=new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN,FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(),SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable To SignIn"));
    }

    @Override
    public void onConversionClicked(User user) {
        Intent intent=new Intent(getApplicationContext(),chatActivity.class);
        intent.putExtra(Constants.KEY_UUSERR,user);
        startActivity(intent);

    }
}