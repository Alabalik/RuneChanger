package com.stirante.RuneChanger.model;

import generated.LolPerksPerkPageResource;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RunePage {
    private final List<Rune> runes = new ArrayList<>(6);
    private final List<Modifier> modifiers = new ArrayList<>(3);
    private String url;
    private String name;
    private Style mainStyle;
    private Style subStyle;

    /**
     * Verifies rune page
     *
     * @return is rune page valid
     */
    public boolean verify() {
        if (getRunes().size() != 6) {
            return false;
        }
        for (Rune rune : runes) {
            if (rune == null) {
                return false;
            }
        }
        for (int i = 0; i < getRunes().size(); i++) {
            Rune rune = getRunes().get(i);
            if (i < 4 && rune.getStyle() != getMainStyle()) {
                System.out.println("Primary path contains runes from another style");
                return false;
            }
            else if (i >= 4 && rune.getStyle() != getSubStyle()) {
                System.out.println("Secondary path contains runes from another style");
                return false;
            }
            if (i < 4 && rune.getSlot() != i) {
                System.out.println("Rune does not belong to this slot");
                return false;
            }
            if (i == 4 && rune.getSlot() == getRunes().get(5).getSlot()) {
                System.out.println("Secondary path contains runes from the same slot");
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "RunePage{" +
                "url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", mainStyle=" + mainStyle +
                ", subStyle=" + subStyle +
                ", runes=" + runes +
                ", modifiers=" + modifiers +
                '}';
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Style getMainStyle() {
        return mainStyle;
    }

    public void setMainStyle(Style mainStyle) {
        this.mainStyle = mainStyle;
    }

    public Style getSubStyle() {
        return subStyle;
    }

    public void setSubStyle(Style subStyle) {
        this.subStyle = subStyle;
    }

    public List<Rune> getRunes() {
        return runes;
    }

    public List<Modifier> getModifiers() {
        return modifiers;
    }

    /**
     * Serializes object to binary format
     *
     * @param out output stream
     */
    public void serialize(DataOutputStream out) throws IOException {
        // version mark just in case
        out.writeByte(0x1);

        // basic data
        out.writeUTF(name);
        out.writeInt(mainStyle.getId());
        out.writeInt(subStyle.getId());

        // list of runes
        out.writeInt(runes.size());
        for (Rune rune : runes) {
            out.writeInt(rune.getId());
        }

        // list of modifiers
        out.writeInt(modifiers.size());
        for (Modifier mod : modifiers) {
            out.writeInt(mod.getId());
        }
    }

    /**
     * Deserializes object from binary format
     *
     * @param in input stream
     */
    public void deserialize(DataInputStream in) throws IOException {
        byte version = in.readByte();
        if (version == 0x1) {
            name = in.readUTF();
            mainStyle = Style.getById(in.readInt());
            subStyle = Style.getById(in.readInt());

            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                runes.add(Rune.getById(in.readInt()));
            }

            size = in.readInt();
            for (int i = 0; i < size; i++) {
                modifiers.add(Modifier.getById(in.readInt()));
            }
        }
        else {
            System.out.println("Unknown rune page version " + version);
        }
    }

    /**
     * Returns RunePage object based on page from client or null if page is wrong or incomplete
     *
     * @param page client rune page
     * @return RunePage object
     */
    public static RunePage fromClient(LolPerksPerkPageResource page) {
        // invalid page
        if (page.selectedPerkIds.size() != 9 || !page.isValid || !page.isEditable) {
            return null;
        }

        RunePage p = new RunePage();

        // copy simple values
        p.name = page.name;
        p.mainStyle = Style.getById(page.primaryStyleId);
        p.subStyle = Style.getById(page.subStyleId);

        // copy selected runes
        for (int i = 0; i < 6; i++) {
            p.runes.add(Rune.getById(page.selectedPerkIds.get(i)));
        }

        // copy selected modifiers
        for (int i = 6; i < 9; i++) {
            p.modifiers.add(Modifier.getById(page.selectedPerkIds.get(i)));
        }

        // final verification
        if (!p.verify()) {
            return null;
        }

        return p;
    }

    /**
     * Fills client rune page with new info
     *
     * @param page client rune page
     */
    public void toClient(LolPerksPerkPageResource page) {
        // limit name to 25 characters (client limit)
        page.name = name.substring(0, Math.min(25, name.length()));
        page.primaryStyleId = mainStyle.getId();
        page.subStyleId = subStyle.getId();
        if (page.selectedPerkIds == null)
		{
			List list = new ArrayList();
			page.selectedPerkIds = list;
		}
        page.selectedPerkIds.clear();
        for (Rune rune : runes) {
            page.selectedPerkIds.add(rune.getId());
        }
        for (Modifier mod : modifiers) {
            page.selectedPerkIds.add(mod.getId());
        }
        page.isActive = true;
    }

}
