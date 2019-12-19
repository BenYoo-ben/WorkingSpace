package com.example.parking;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.client.Firebase;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;


public class Login extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener{
    SignInButton Google_Login;

    private static final int RC_SIGN_IN = 1000;
    private FirebaseAuth mAuth;
    private GoogleApiClient mGoogleApiClient;
    private GoogleSignInClient mGoogleSignInClient;

    @Override

    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);


  //ML Kit test working fine.
//dezeen_South-Korean-road-signs-by-Studio-Dumbar.jpg
        /*Bitmap bMap = BitmapFactory.decodeFile(
                Environment.getExternalStorageDirectory()
                        .getAbsolutePath()+"/Download/plate.jpg" );
        MLKit mlk = new MLKit(getApplicationContext());
        mlk.runTextRecognition(bMap);



        startActivity(new Intent(this,CameraMain.class));
*/


        setContentView(R.layout.login);


        mAuth = FirebaseAuth.getInstance();

        Firebase.setAndroidContext(this);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser!=null) {
            startLoading(currentUser.getUid());
        }
       Google_Login = findViewById(R.id.Google_Login);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
       mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

       Google_Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                signInIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(null, "Google sign in failed", e);
                // ...
            }
        }
    }
    void firebaseAuthWithGoogle(GoogleSignInAccount acct){
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(),null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(getApplicationContext(), "인증 실패", Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(getApplicationContext(), "구글 로그인 인증 성공", Toast.LENGTH_SHORT).show();

                          startLoading(mAuth.getCurrentUser().getUid());
                        }
                    }
                });
    }
    void startA(String userID)
    {
        FirebaseController.userID =userID;
        startActivity(new Intent(this,ActivityA.class));
    }
    void startLoading(String userID)
    {
        FirebaseController.userID =userID;
        startActivity(new Intent(this,Loading.class));
        this.finish();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }



}