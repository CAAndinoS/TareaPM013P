package com.example.tareapm013p;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TransActivity extends AppCompatActivity {
    private DatabaseReference mDatabase;
    private StorageReference mStorage;
    ImageView ivPhoto;
    Button btnActualizar,btnEliminar,btnFoto;
    EditText etNombres, etApellidos, etNacimiento;
    String selectedUserId;
    String selectedUserPhotoUrl;

    static final int REQUEST_IMAGE = 101;
    static final int PETICION_ACCESS_CAM = 201;
    String currentPhotoPath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trans);

        ControlsSet();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorage = FirebaseStorage.getInstance().getReference();
        DatabaseReference usersRef = mDatabase.child("users");

        // Obtener los datos del usuario seleccionado enviados desde ListActivity
        Intent intent = getIntent();
        selectedUserId = intent.getStringExtra("selected_user_id");
        String selectedUserFirstName = intent.getStringExtra("selected_user_first_name");
        String selectedUserLastName = intent.getStringExtra("selected_user_last_name");
        String selectedUserBirthdate = intent.getStringExtra("selected_user_birthdate");
        selectedUserPhotoUrl = intent.getStringExtra("selected_user_photo_url");


        etNombres.setText(selectedUserFirstName);
        etApellidos.setText(selectedUserLastName);
        etNacimiento.setText(selectedUserBirthdate);
        // cargar la imagen desde la URL usando una biblioteca como Glide o Picasso
        Glide.with(this).load(selectedUserPhotoUrl).into(ivPhoto);

        btnActualizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newFirstName = etNombres.getText().toString();
                String newLastName = etApellidos.getText().toString();
                String newBirthdate = etNacimiento.getText().toString();

                User newUser = new User(selectedUserId, newFirstName, newLastName, newBirthdate, selectedUserPhotoUrl);

                if (currentPhotoPath != null) {
                    // Subir la nueva foto a Firebase Storage y guardar la URL en la base de datos
                    Uri file = Uri.fromFile(new File(currentPhotoPath));
                    StorageReference photoRef = mStorage.child("users").child(selectedUserId + ".jpg");

                    photoRef.putFile(file)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    // Obtener la URL de la foto subida
                                    photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            // Guardar la URL de la foto en el objeto User
                                            newUser.setPhotoUrl(uri.toString());

                                            // Actualizar los datos del usuario en la base de datos
                                            mDatabase.child("users").child(selectedUserId).setValue(newUser, new DatabaseReference.CompletionListener() {
                                                @Override
                                                public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                                    if (error == null) {
                                                        Toast.makeText(TransActivity.this, "Datos actualizados exitosamente", Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Toast.makeText(TransActivity.this, "Error al actualizar los datos", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                        }
                                    });
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Mostrar un mensaje de error
                                    Toast.makeText(TransActivity.this, "Error al subir la foto", Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    // Si no se ha tomado una nueva foto, actualizar los datos del usuario en la base de datos sin cambiar la foto
                    mDatabase.child("users").child(selectedUserId).setValue(newUser, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if (error == null) {
                                Toast.makeText(TransActivity.this, "Datos actualizados exitosamente", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(getApplicationContext(), ListActivity.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(TransActivity.this, "Error al actualizar los datos", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });

        btnEliminar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                usersRef.child(selectedUserId).removeValue();
                finish();
            }
        });

        btnFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                permisos();
            }
        });

    }
    private void permisos() {
        //Metodo para obtener los permisos requeridos de la aplicacion
        if(ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PETICION_ACCESS_CAM);
        }else{
            //TomarFoto();
            dispatchTakePictureIntent();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.toString();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.tareapm013p.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if(requestCode == REQUEST_IMAGE && resultCode == RESULT_OK)
        {

            if (currentPhotoPath != null) {
                try {
                    File foto = new File(currentPhotoPath);
                    Bitmap bitmap = BitmapFactory.decodeFile(foto.getAbsolutePath());
                    ivPhoto.setImageBitmap(bitmap);
                }
                catch (Exception ex)
                {
                    ex.toString();
                }
            }
        }
    }
    private void ControlsSet() {
        ivPhoto = findViewById(R.id.ivPhoto);
        btnActualizar = findViewById(R.id.btnActualizar);
        btnEliminar = findViewById(R.id.btnEliminar);
        btnFoto = findViewById(R.id.btnFoto);
        etNombres = findViewById(R.id.etNombres);
        etApellidos = findViewById(R.id.etApellidos);
        etNacimiento = findViewById(R.id.etNacimiento);
    }
}