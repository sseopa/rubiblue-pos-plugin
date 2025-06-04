declare module '@capacitor/core' {
    interface CapacitorPlugins {
        GlocellPosModule: GlocellPosModulePlugin;
    }
}
export interface GlocellPosModulePlugin {
    getSerial(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
    print(options: {
        ReceiptText: string;
        ReceiptLogo: string;
    }): Promise<{
        results: any[];
    }>;
}
