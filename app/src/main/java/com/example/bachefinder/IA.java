package com.example.bachefinder;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class IA extends AppCompatActivity {
    private static final String TAG = "ValidacionImagen";
    public TextView textResponse;
    private ImageView imageView;
    private ActivityResultLauncher<Intent> launcher;
    private Bitmap imageBitmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ia);

        textResponse = findViewById(R.id.text_validate_photo);
        imageView = findViewById(R.id.id_view_photo);

        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            Intent data = result.getData();
                            Bitmap bitmap = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
                            Bitmap rotatedBitmap = rotateBitmap(bitmap, 90);
                            Bitmap scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, 100, 100, true);
                            imageBitmap = convertToGray(scaledBitmap);
                            imageView.setImageBitmap(imageBitmap);
                        }
                    }
                }
        );

        Button takePhoto = findViewById(R.id.btn_take_photo);
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                launcher.launch(takePictureIntent);
            }
        });

        Button validatePhoto = findViewById(R.id.btn_validate_photo);
        validatePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imageBitmap != null) {
                    sendRequest();
                } else {
                    textResponse.setText("Debes tomar primero una foto.");
                }

            }
        });
    }

    private Bitmap rotateBitmap(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private Bitmap convertToGray(Bitmap original) {
        Bitmap gris = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                int color = original.getPixel(x, y);
                int rojo = Color.red(color);
                int verde = Color.green(color);
                int azul = Color.blue(color);
                int grisColor = (int) (0.299 * rojo + 0.587 * verde + 0.114 * azul);
                int nuevoColor = Color.rgb(grisColor, grisColor, grisColor);
                gris.setPixel(x, y, nuevoColor);
            }
        }
        return gris;
    }

    private void sendRequest() {
        OkHttpClient client = new OkHttpClient();

        // String url = "http://192.168.100.15:8000/predict";
        String url = "http://172.20.10.3:8000/predict";

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "hueco.jpg", RequestBody.create(MediaType.parse("image/jpeg"), byteArray))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textResponse.setText("No fue posible conectar con el backend");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        validateResponse(responseBody);
                    }
                });
            }
        });
    }

    public void  validateResponse(String responseBody){
        Log.d(TAG, responseBody);
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
        JsonArray jsonArray = (JsonArray) jsonObject.get("prediction").getAsJsonArray().get(0);
        int position = validateMaximum(jsonArray);

        String[] listaTextos = {"Bache", "Descascaramiento", "Fisura en bloque",
                "Fisura por deslizamiento", "Fisura por reflexión",
                "Fisura transversal", "Fisura longitudinales y transversales",
                "Hundimiento", "Parche", "Perdida de agregado",
                "Piel de cocodrilo"};
        textResponse.setText(listaTextos[position]);

    }

    public static int validateMaximum(JsonArray list) {
        int position = 0;
        double max = list.get(0).getAsDouble();

        for (int i = 1; i < list.size(); i++) {
            double valorActual = list.get(i).getAsDouble();
            if (valorActual > max) {
                max = valorActual;
                position = i;
            }
        }
        return position;
    }
}