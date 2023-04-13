package com.example.tareapm013p;

import static android.content.ContentValues.TAG;

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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserActivity extends AppCompatActivity {
    private DatabaseReference mDatabase;
    private StorageReference mStorage;

    ImageView ivPhoto;
    Button btnGuardar,btnLista,btnFoto;
    EditText etNombres, etApellidos, etNacimiento;
    static final int REQUEST_IMAGE = 101;
    static final int PETICION_ACCESS_CAM = 201;
    String currentPhotoPath;
    int lastUserId = 0; // variable que guarda el último ID generado


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        ControlsSet();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorage = FirebaseStorage.getInstance().getReference();

        // Obtener el último ID generado para los usuarios
        mDatabase.child("lastUserId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    lastUserId = snapshot.getValue(Integer.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error retrieving lastUserId", error.toException());
            }
        });

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Obtener los valores de los campos de entrada
                String nombres = etNombres.getText().toString();
                String apellidos = etApellidos.getText().toString();
                String nacimiento = etNacimiento.getText().toString();

                // Crear un objeto User con los valores de entrada
                User user = new User(Integer.toString(lastUserId + 1), nombres, apellidos, nacimiento, null);

                // Incrementar el último ID generado para los usuarios
                lastUserId++;

                // Subir la foto a Firebase Storage y guardar la URL en la base de datos
                if (currentPhotoPath != null) {
                    Uri file = Uri.fromFile(new File(currentPhotoPath));
                    StorageReference photoRef = mStorage.child("users").child(user.getId() + ".jpg");

                    photoRef.putFile(file)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    // Obtener la URL de la foto subida
                                    photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            // Guardar la URL de la foto en el objeto User
                                            user.setPhotoUrl(uri.toString());

                                            // Guardar los datos del usuario en la base de datos
                                            addUser(user);
                                        }
                                    });
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Mostrar un mensaje de error
                                    Toast.makeText(UserActivity.this, "Error al subir la foto", Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    // Si no se ha tomado una foto, guardar los datos del usuario en la base de datos sin la foto
                    addUser(user);
                }
            }
        });

        btnFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                permisos();
            }
        });

        btnLista.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ListActivity.class);
                startActivity(intent);
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
        btnGuardar = findViewById(R.id.btnGuardar);
        btnLista = findViewById(R.id.btnLista);
        btnFoto = findViewById(R.id.btnFoto);
        etNombres = findViewById(R.id.etNombres);
        etApellidos = findViewById(R.id.etApellidos);
        etNacimiento = findViewById(R.id.etNacimiento);
    }

    private void addUser(User user) {
        // Guardar el último ID generado para los usuarios
        mDatabase.child("lastUserId").setValue(lastUserId);

        // Guardar los datos del usuario en la base de datos
        mDatabase.child("users").child(user.getId()).setValue(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Mostrar un mensaje de éxito
                        Toast.makeText(UserActivity.this, "Usuario agregado correctamente", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Mostrar un mensaje de error
                        Toast.makeText(UserActivity.this, "Error al agregar el usuario", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error adding user to database", e);
                    }
                });
    }


    private void uploadPhoto(Uri photoUri, String userId) {
        // Subir la foto del usuario al almacenamiento
        StorageReference photoRef = mStorage.child("users").child(userId + ".jpg");
        photoRef.putFile(photoUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Actualizar la URL de la foto en la base de datos
                photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri downloadUrl) {
                        mDatabase.child("users").child(userId).child("photoUrl").setValue(downloadUrl.toString());
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "Error uploading photo.", e);
            }
        });
    }
}