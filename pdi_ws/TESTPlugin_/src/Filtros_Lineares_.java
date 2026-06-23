import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class Filtros_Lineares_ implements PlugIn {

    private ImagePlus imgAtual;
    private final String[] METODOS = {
            "Média (Passa-baixas): Suaviza a imagem e tende a minimizar ruídos",
            "Passa-altas: Realça o contraste e destaca as bordas",
            "Realce de Borda (Norte): Realça linhas e bordas na direção Norte"
        };

    @Override
    public void run(String arg) {
        imgAtual = IJ.getImage();

        if (imgAtual == null || imgAtual.getType() != ImagePlus.GRAY8) {
            IJ.error("Erro", "Abra uma imagem de 8-bits (tons de cinza).");
            return;
        }

        apresentarInterfaceGrafica();
    }

    private void apresentarInterfaceGrafica() {
        GenericDialog gd = new GenericDialog("Filtros Lineares 3x3");
        gd.addRadioButtonGroup("Selecione o Filtro:", METODOS, 3, 1, METODOS[0]);
        gd.showDialog();

        if (gd.wasCanceled()) return;

        String metodoSelecionado = gd.getNextRadioButton();
        ImageProcessor procOriginal = imgAtual.getProcessor();
        ImageProcessor procModificado = procOriginal.duplicate();

        // Definição dos Kernels
        double[][] kernelMedia = {
            {1, 1, 1},
            {1, 1, 1},
            {1, 1, 1}
        };

        double[][] kernelPassaAltas = {
            {-1, -1, -1},
            {-1,  8, -1},
            {-1, -1, -1}
        };

        double[][] kernelBordaNorte = {
            { 1,  1,  1},
            { 1, -2,  1},
            {-1, -1, -1}
        };

        if (metodoSelecionado.equals(METODOS[0])) {
            // Filtro de Média divide a soma por 9 em Kernel 3x3
            aplicarConvolucao(procOriginal, procModificado, kernelMedia, 9.0);
        } else if (metodoSelecionado.equals(METODOS[1])) {
            aplicarConvolucao(procOriginal, procModificado, kernelPassaAltas, 1.0);
        } else {
            aplicarConvolucao(procOriginal, procModificado, kernelBordaNorte, 1.0);
        }

        imgAtual.setProcessor(procModificado);
        imgAtual.updateAndDraw();
    }

    /**
     * Método genérico para aplicar convolução com qualquer kernel 3x3.
     * Detalhe importante: o método recebe "divisor" pra ficar genérico, mas 
     * somente o filtro de média vai usar nesse caso
     */
    private void aplicarConvolucao(ImageProcessor original, ImageProcessor modificado, double[][] kernel, double divisor) {
        int largura = original.getWidth();
        int altura = original.getHeight();

        // Ignorando as bordas extremas
        // O for de fora está sempre apontando para o "pixel alvo"
        for (int x = 1; x < largura - 1; x++) {
            for (int y = 1; y < altura - 1; y++) {
                
                double soma = 0;
                
                // Percorrendo a vizinhança - Como o for de fora aponta para o alvo, o de dentro tem que pegar os pixels em volta do alvo
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        int pixel = original.getPixel(x + i, y + j);
                        soma += pixel * kernel[i + 1][j + 1];
                    }
                }
                
                soma = soma / divisor;

                soma = limitar(soma);

                modificado.putPixel(x, y, (int) soma);
            }
        }
    }
    
    // Método auxiliar para evitar estouro de limite de cor
    private int limitar(double valor) {
        if (valor > 255) return 255;
        if (valor < 0) return 0;
        return (int) valor;
    }
}