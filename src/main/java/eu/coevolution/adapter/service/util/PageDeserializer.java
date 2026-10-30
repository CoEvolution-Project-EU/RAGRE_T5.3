package eu.coevolution.adapter.service.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Type;
import java.util.List;

public class PageDeserializer<T> implements JsonDeserializer<Page<T>> {

    private final Class<T> contentClass;

    public PageDeserializer(Class<T> contentClass) {
        this.contentClass = contentClass;
    }

    @Override
    public Page<T> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        int number = jsonObject.get("number").getAsInt();
        int size = jsonObject.get("size").getAsInt();
        long totalElements = jsonObject.get("totalElements").getAsLong();
        JsonArray contentJson = jsonObject.getAsJsonArray("content");

        List<T> content = context.deserialize(contentJson, TypeToken.getParameterized(List.class, contentClass).getType());
        Pageable pageable = PageRequest.of(number, size);

        return new PageImpl<>(content, pageable, totalElements);
    }
}
