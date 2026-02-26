package ragamuffin.building;

import ragamuffin.world.BlockType;
import ragamuffin.world.LandmarkType;

/**
 * Defines what materials are dropped when blocks are broken.
 */
public class BlockDropTable {

    /**
     * Get the material dropped when a block is broken.
     * @param blockType The type of block broken
     * @param landmark The landmark this block belongs to (null if not part of a landmark)
     * @return The material dropped, or null if nothing is dropped
     */
    public Material getDrop(BlockType blockType, LandmarkType landmark) {
        // Special drops for landmark blocks
        if (landmark != null) {
            return getLandmarkDrop(blockType, landmark);
        }

        // Standard block drops
        switch (blockType) {
            case TREE_TRUNK:
                return Material.WOOD;
            case BRICK:
                return Material.BRICK;
            case GLASS:
                return Material.GLASS;
            case STONE:
                return Material.STONE;
            case GRASS:
                return Material.GRASS_TURF;
            case DIRT:
                return Material.DIRT;
            case PAVEMENT:
                // 5% chance of a dropped coin found in the cracks
                if (Math.random() < 0.05) {
                    return Math.random() < 0.7 ? Material.PENNY : Material.SHILLING;
                }
                return Material.PAVEMENT_SLAB;
            case ROAD:
                // 5% chance of a dropped coin on the road
                if (Math.random() < 0.05) {
                    return Math.random() < 0.7 ? Material.PENNY : Material.SHILLING;
                }
                return Material.ROAD_ASPHALT;
            case WOOD:
                return Material.WOOD;
            case WOOD_PLANKS:
                return Material.PLANKS;
            case STAIRS:
                return Material.STAIRS;
            case LADDER:
                return Material.LADDER;
            case HALF_BLOCK:
                return Material.HALF_BLOCK;
            case CARDBOARD:
                return Material.CARDBOARD;
            case CONCRETE:
                return Material.CONCRETE;
            case ROOF_TILE:
                return Material.ROOF_TILE;
            case TARMAC:
                return Material.TARMAC;
            case CORRUGATED_METAL:
                return Material.SCRAP_METAL;
            case METAL_RED:
                return Material.METAL_RED;
            case RENDER_WHITE:
                return Material.RENDER;
            case RENDER_CREAM:
                return Material.RENDER_CREAM;
            case RENDER_PINK:
                return Material.RENDER_PINK;
            case SLATE:
                return Material.SLATE;
            case PEBBLEDASH:
                return Material.PEBBLEDASH;
            case DOOR_WOOD:
            case DOOR_LOWER:
            case DOOR_UPPER:
                return Material.DOOR;
            case LINOLEUM:
                return Material.LINOLEUM;
            case LINO_GREEN:
                return Material.LINO_GREEN;
            case YELLOW_BRICK:
                return Material.YELLOW_BRICK;
            case TILE_WHITE:
                return Material.TILE;
            case TILE_BLACK:
                return Material.TILE_BLACK;
            case COUNTER:
                return Material.COUNTER;
            case SHELF:
                return Material.SHELF;
            case BOOKSHELF:
                return Material.BOOKSHELF;
            case TABLE:
                return Material.TABLE;
            case CARPET:
                return Material.CARPET;
            case IRON_FENCE:
                return Material.FENCE;
            case WOOD_FENCE:
            case WOOD_WALL:
                return Material.WOOD;
            case SIGN_WHITE:
                return Material.SIGN;
            case SIGN_RED:
                return Material.SIGN_RED;
            case SIGN_BLUE:
                return Material.SIGN_BLUE;
            case SIGN_GREEN:
                return Material.SIGN_GREEN;
            case SIGN_YELLOW:
                return Material.SIGN_YELLOW;
            case GARDEN_WALL:
                return Material.GARDEN_WALL;
            case LEAVES:
                // 30% chance to drop WOOD (twigs/branches), 70% chance to drop nothing
                return (Math.random() < 0.30) ? Material.WOOD : null;
            case COAL_ORE:
                return Material.COAL;
            case IRON_ORE:
                return Material.IRON;
            case FLINT:
                return Material.FLINT;
            case AIR:
            case WATER:
            default:
                return null; // No drop
        }
    }

    /**
     * Get special drops for blocks that are part of landmarks.
     */
    private Material getLandmarkDrop(BlockType blockType, LandmarkType landmark) {
        if (landmark == LandmarkType.JEWELLER) {
            // Jeweller blocks drop diamond
            if (blockType == BlockType.GLASS || blockType == BlockType.BRICK) {
                return Material.DIAMOND;
            }
        } else if (landmark == LandmarkType.OFFICE_BUILDING) {
            // Office building blocks drop office materials
            if (blockType == BlockType.BRICK) {
                return Material.COMPUTER;
            } else if (blockType == BlockType.GLASS) {
                return Material.OFFICE_CHAIR;
            }
        } else if (landmark == LandmarkType.GREGGS) {
            // Greggs blocks drop a varied haul of fine British bakery goods
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS
                    || blockType == BlockType.COUNTER || blockType == BlockType.SHELF) {
                double roll = Math.random();
                if (roll < 0.35) return Material.SAUSAGE_ROLL;
                else if (roll < 0.65) return Material.STEAK_BAKE;
                else return Material.ENERGY_DRINK; // chilled from the fridge
            }
        } else if (landmark == LandmarkType.CHIPPY) {
            // Chippy drops chips
            if (blockType == BlockType.STONE || blockType == BlockType.BRICK) {
                return Material.CHIPS;
            }
        } else if (landmark == LandmarkType.KEBAB_SHOP) {
            // Kebab shop drops kebabs
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Material.KEBAB;
            }
        } else if (landmark == LandmarkType.OFF_LICENCE) {
            // Off-licence drops energy drinks and crisps
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Math.random() < 0.5 ? Material.ENERGY_DRINK : Material.CRISPS;
            }
        } else if (landmark == LandmarkType.TESCO_EXPRESS || landmark == LandmarkType.CORNER_SHOP) {
            // Supermarket/corner shop drops tinned food
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                double roll = Math.random();
                if (roll < 0.3) return Material.TIN_OF_BEANS;
                else if (roll < 0.6) return Material.CRISPS;
                else return Material.ENERGY_DRINK;
            }
        } else if (landmark == LandmarkType.CHARITY_SHOP) {
            // Charity shop drops cardboard
            if (blockType == BlockType.BRICK || blockType == BlockType.WOOD) {
                return Material.CARDBOARD;
            }
        } else if (landmark == LandmarkType.PUB || landmark == LandmarkType.WETHERSPOONS) {
            // Pub/Spoons drops pints
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Material.PINT;
            }
        } else if (landmark == LandmarkType.NANDOS) {
            // Nandos drops peri-peri chicken
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Material.PERI_PERI_CHICKEN;
            }
        } else if (landmark == LandmarkType.BOOKIES || landmark == LandmarkType.BETTING_SHOP) {
            // Bookies drops scratch cards
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Material.SCRATCH_CARD;
            }
        } else if (landmark == LandmarkType.NEWSAGENT) {
            // Newsagent drops newspapers and crisps
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Math.random() < 0.5 ? Material.NEWSPAPER : Material.CRISPS;
            }
        } else if (landmark == LandmarkType.LAUNDERETTE) {
            // Launderette drops washing powder
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Material.WASHING_POWDER;
            }
        } else if (landmark == LandmarkType.GP_SURGERY) {
            // GP surgery drops paracetamol
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Material.PARACETAMOL;
            }
        } else if (landmark == LandmarkType.PRIMARY_SCHOOL || landmark == LandmarkType.LIBRARY) {
            // School/library drops textbooks
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Material.TEXTBOOK;
            }
        } else if (landmark == LandmarkType.CHURCH) {
            // Church drops hymn books
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Material.HYMN_BOOK;
            }
        } else if (landmark == LandmarkType.PETROL_STATION) {
            // Petrol station drops petrol cans and energy drinks
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Math.random() < 0.5 ? Material.PETROL_CAN : Material.ENERGY_DRINK;
            }
        } else if (landmark == LandmarkType.BARBER) {
            // Barber drops hair clippers
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Material.HAIR_CLIPPERS;
            }
        } else if (landmark == LandmarkType.NAIL_SALON) {
            // Nail salon drops nail polish
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Material.NAIL_POLISH;
            }
        } else if (landmark == LandmarkType.PHONE_REPAIR) {
            // Phone repair drops broken phones
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Material.BROKEN_PHONE;
            }
        } else if (landmark == LandmarkType.CASH_CONVERTER || landmark == LandmarkType.PAWN_SHOP) {
            // Cash Converter/pawn shop drops dodgy DVDs, broken phones, and loose change
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                double roll = Math.random();
                if (roll < 0.33) return Material.DODGY_DVD;
                else if (roll < 0.66) return Material.BROKEN_PHONE;
                else return Math.random() < 0.5 ? Material.SHILLING : Material.PENNY;
            }
        } else if (landmark == LandmarkType.FIRE_STATION) {
            // Fire station drops fire extinguishers
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Material.FIRE_EXTINGUISHER;
            }
        } else if (landmark == LandmarkType.BUILDERS_MERCHANT || landmark == LandmarkType.WAREHOUSE) {
            // Builders merchant/warehouse drops plywood and pipe
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Math.random() < 0.5 ? Material.PLYWOOD : Material.PIPE;
            }
        } else if (landmark == LandmarkType.INDUSTRIAL_ESTATE) {
            // Industrial estate drops scrap metal and plywood
            if (blockType == BlockType.BRICK || blockType == BlockType.CORRUGATED_METAL) {
                return Math.random() < 0.5 ? Material.SCRAP_METAL : Material.PLYWOOD;
            }
        } else if (landmark == LandmarkType.COMMUNITY_CENTRE) {
            // Community centre drops cardboard and chairs
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Math.random() < 0.5 ? Material.CARDBOARD : Material.OFFICE_CHAIR;
            }
        } else if (landmark == LandmarkType.JOB_CENTRE) {
            // Job centre drops staplers and computers
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Math.random() < 0.5 ? Material.STAPLER : Material.COMPUTER;
            }
        }

        // If no special landmark drop, fall back to standard drop
        return getDrop(blockType, null);
    }
}
