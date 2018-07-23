package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.base.GuiSchematicBrowserBase;
import fi.dy.masa.litematica.gui.interfaces.ISelectionListener;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.malilib.gui.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3i;

public class WidgetSchematicBrowser extends WidgetFileBrowserBase
{
    protected final Map<File, SchematicMetadata> cachedMetadata = new HashMap<>();
    protected final Map<File, Pair<ResourceLocation, DynamicTexture>> cachedPreviewImages = new HashMap<>();
    protected final GuiSchematicBrowserBase parent;
    protected final int infoWidth;
    protected final int infoHeight;

    public WidgetSchematicBrowser(int x, int y, int width, int height, GuiSchematicBrowserBase parent, @Nullable ISelectionListener<DirectoryEntry> selectionListener)
    {
        super(x, y, width, height, parent.getBrowserContext(), parent.getDefaultDirectory(), parent, selectionListener);

        this.title = I18n.format("litematica.gui.title.schematic_browser");
        this.infoWidth = 170;
        this.infoHeight = 280;
        this.parent = parent;
    }

    @Override
    protected int getBrowserWidthForTotalWidth(int width)
    {
        return super.getBrowserWidthForTotalWidth(width) - this.infoWidth;
    }

    @Override
    public void onGuiClosed()
    {
        super.onGuiClosed();

        this.clearPreviewImages();
    }

    private void clearPreviewImages()
    {
        for (Pair<ResourceLocation, DynamicTexture> pair : this.cachedPreviewImages.values())
        {
            this.mc.getTextureManager().deleteTexture(pair.getLeft());
        }
    }

    @Override
    protected File getRootDirectory()
    {
        return DataManager.ROOT_SCHEMATIC_DIRECTORY;
    }

    @Override
    protected void addFileEntriesToList(File dir, List<DirectoryEntry> list)
    {
        for (File file : dir.listFiles(SCHEMATIC_FILTER))
        {
            list.add(new DirectoryEntry(DirectoryEntryType.fromFile(file), dir, file.getName()));
        }

        Collections.sort(list);
    }

    @Override
    protected void drawAdditionalContents(int mouseX, int mouseY)
    {
        this.drawSelectedSchematicInfo(this.getSelectedEntry());
    }

    protected void drawSelectedSchematicInfo(@Nullable DirectoryEntry entry)
    {
        int x = this.posX + this.totalWidth - this.infoWidth;
        int y = this.posY;

        RenderUtils.drawOutlinedBox(x, y, this.infoWidth, this.infoHeight, 0xA0000000, COLOR_HORIZONTAL_BAR);

        if (entry == null)
        {
            return;
        }

        SchematicMetadata meta = this.getSchematicMetadata(entry);

        if (meta != null)
        {
            x += 3;
            y += 3;
            int textColor = 0xC0C0C0C0;
            int valueColor = 0xC0FFFFFF;

            String str = I18n.format("litematica.gui.label.schematic_info.name");
            this.fontRenderer.drawString(str, x, y, textColor);
            y += 12;

            this.fontRenderer.drawString(meta.getName(), x + 4, y, valueColor);
            y += 12;

            str = I18n.format("litematica.gui.label.schematic_info.author", meta.getAuthor());
            this.fontRenderer.drawString(str, x, y, textColor);
            y += 12;

            String strDate = DATE_FORMAT.format(new Date(meta.getTimeCreated()));
            str = I18n.format("litematica.gui.label.schematic_info.time_created", strDate);
            this.fontRenderer.drawString(str, x, y, textColor);
            y += 12;

            if (meta.hasBeenModified())
            {
                strDate = DATE_FORMAT.format(new Date(meta.getTimeModified()));
                str = I18n.format("litematica.gui.label.schematic_info.time_modified", strDate);
                this.fontRenderer.drawString(str, x, y, textColor);
                y += 12;
            }

            str = I18n.format("litematica.gui.label.schematic_info.region_count", meta.getRegionCount());
            this.fontRenderer.drawString(str, x, y, textColor);
            y += 12;

            str = I18n.format("litematica.gui.label.schematic_info.total_volume", meta.getTotalVolume());
            this.fontRenderer.drawString(str, x, y, textColor);
            y += 12;

            str = I18n.format("litematica.gui.label.schematic_info.total_blocks", meta.getTotalBlocks());
            this.fontRenderer.drawString(str, x, y, textColor);
            y += 12;

            str = I18n.format("litematica.gui.label.schematic_info.enclosing_size");
            this.fontRenderer.drawString(str, x, y, textColor);
            y += 12;

            Vec3i areaSize = meta.getEnclosingSize();
            String tmp = String.format("%d x %d x %d", areaSize.getX(), areaSize.getY(), areaSize.getZ());
            this.fontRenderer.drawString(tmp, x + 4, y, valueColor);
            y += 12;

            /*
            str = I18n.format("litematica.gui.label.schematic_info.description");
            this.fontRenderer.drawString(str, x, y, textColor);
            */
            y += 12;

            Pair<ResourceLocation, DynamicTexture> pair = this.cachedPreviewImages.get(entry.getFullPath());

            if (pair != null)
            {
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                this.bindTexture(pair.getLeft());

                int iconSize = (int) Math.sqrt(pair.getRight().getTextureData().length);
                Gui.drawModalRectWithCustomSizedTexture(x + 10, y, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
            }
        }
    }

    public void clearSchematicMetadataCache()
    {
        this.clearPreviewImages();
        this.cachedMetadata.clear();
        this.cachedPreviewImages.clear();
    }

    @Nullable
    protected SchematicMetadata getSchematicMetadata(DirectoryEntry entry)
    {
        File file = new File(entry.getDirectory(), entry.getName() + "_meta");
        SchematicMetadata meta = this.cachedMetadata.get(file);

        if (meta == null)
        {
            meta = SchematicMetadata.fromFile(file);

            if (meta != null)
            {
                this.cachedMetadata.put(file, meta);
                this.createPreviewImage(new File(entry.getDirectory(), entry.getName()), meta);
            }
        }

        return meta;
    }

    private void createPreviewImage(File file, SchematicMetadata meta)
    {
        int[] previewImageData = meta.getPreviewImagePixelData();

        if (previewImageData != null && previewImageData.length > 0)
        {
            try
            {
                int size = (int) Math.sqrt(previewImageData.length);

                if (size * size == previewImageData.length)
                {
                    //BufferedImage buf = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
                    //buf.setRGB(0, 0, size, size, previewImageData, 0, size);

                    DynamicTexture tex = new DynamicTexture(size, size);
                    ResourceLocation rl = new ResourceLocation("litematica", file.getAbsolutePath());
                    this.mc.getTextureManager().loadTexture(rl, tex);

                    System.arraycopy(previewImageData, 0, tex.getTextureData(), 0, previewImageData.length);
                    tex.updateDynamicTexture();

                    this.cachedPreviewImages.put(file, Pair.of(rl, tex));
                }
            }
            catch (Exception e)
            {
            }
        }
    }
}
