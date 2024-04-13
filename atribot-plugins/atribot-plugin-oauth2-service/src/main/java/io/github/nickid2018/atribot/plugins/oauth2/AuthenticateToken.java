package io.github.nickid2018.atribot.plugins.oauth2;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class AuthenticateToken {

    @DatabaseField(id = true, columnName = "id", dataType = DataType.STRING, canBeNull = false, unique = true, index = true)
    public String id;
    @DatabaseField(columnName = "access_token", dataType = DataType.STRING, canBeNull = false)
    public String accessToken;
    @DatabaseField(columnName = "expire_time", dataType = DataType.LONG, canBeNull = false)
    public long expireTime;
    @DatabaseField(columnName = "refresh_token", dataType = DataType.STRING)
    public String refreshToken;
    @DatabaseField(columnName = "scopes", dataType = DataType.STRING)
    public String scopes;
}
