package io.github.nickid2018.atribot.core.message.persist;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import io.github.nickid2018.atribot.network.message.MessageChain;
import io.github.nickid2018.atribot.network.message.TargetData;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@DatabaseTable(tableName = "message_queue")
public class MessageQueueEntry {

    @DatabaseField(id = true, columnName = "id", dataType = DataType.STRING, canBeNull = false, unique = true, index = true)
    public String id;
    @DatabaseField(columnName = "backend_id", dataType = DataType.STRING, canBeNull = false)
    public String backendID;
    @DatabaseField(columnName = "send_target", dataType = DataType.SERIALIZABLE, canBeNull = false)
    public TargetData sendTarget;
    @DatabaseField(columnName = "message_chain", dataType = DataType.SERIALIZABLE, canBeNull = false)
    public MessageChain messageChain;
    @DatabaseField(columnName = "send_time", dataType = DataType.LONG, canBeNull = false)
    public long sendTime;
}
