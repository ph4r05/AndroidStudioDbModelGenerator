package net.phonex.intellij.android.dbmodel.typeserializers;

import com.intellij.psi.PsiType;
import net.phonex.intellij.android.dbmodel.typeserializers.serializers.ParcelableObjectSerializer;
import net.phonex.intellij.android.dbmodel.util.PsiUtils;

/**
 * Serializer factory for Parcelable objects
 *
 * @author Dallas Gutauckis [dallas@gutauckis.com]
 */
public class ParcelableSerializerFactory implements TypeSerializerFactory {

    private ParcelableObjectSerializer mSerializer = new ParcelableObjectSerializer();

    @Override
    public TypeSerializer getSerializer(PsiType psiType) {
        if (PsiUtils.isOfType(psiType, "android.os.Parcelable")) {
            return mSerializer;
        }

        return null;
    }
}
