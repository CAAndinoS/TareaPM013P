package com.example.tareapm013p;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

public class UserAdapter extends ArrayAdapter<User> {
    private Context contexto;
    private List<User> listaUsuarios;

    public UserAdapter(Context contexto, List<User> listaUsuarios) {
        super(contexto, R.layout.list_item_user, listaUsuarios);
        this.contexto = contexto;
        this.listaUsuarios = listaUsuarios;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(contexto);
        View view = inflater.inflate(R.layout.list_item_user, null);

        ImageView imagenUsuario = view.findViewById(R.id.foto_image_view);
        TextView idUsuario = view.findViewById(R.id.id_text_view);
        TextView nombreUsuario = view.findViewById(R.id.nombre_text_view);
        TextView fechaNacimiento = view.findViewById(R.id.fecha_nacimiento_text_view);

        User usuario = listaUsuarios.get(position);
        idUsuario.setText(usuario.getId());
        nombreUsuario.setText(usuario.getFirstName() + " " + usuario.getLastName());
        fechaNacimiento.setText(usuario.getBirthdate());


        Glide.with(contexto)
                .load(usuario.getPhotoUrl())
                .into(imagenUsuario);

        return view;
    }
}
