import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class Morfologia_Matematica_ implements PlugIn {

    private ImagePlus imgAberta;
    
    private final String[] METODOS = {
        "Dilatação: Expande contornos e fecha pequenos orifícios",
        "Erosão: Remove pequenos detalhes e afina os contornos",
        "Abertura: Suaviza contornos e rompe istmos (Erosão seguida de Dilatação)",
        "Fechamento: Funde descontinuidades e elimina buracos (Dilatação seguida de Erosão)",
        "Extração de Borda: Diferença entre a imagem original e sua erosão",
        "Esqueletização: Aplicação iterativa de erosões e aberturas"
    };

    // ==========================================
    // 1. PONTO DE ENTRADA E VALIDAÇÃO
    // ==========================================
    @Override
    public void run(String arg) {
        imgAberta = IJ.getImage();

        if (imgAberta == null || imgAberta.getType() != ImagePlus.GRAY8) {
            IJ.error("Erro", "Abra uma imagem binária de 8-bits.");
            return;
        }

        apresentarInterfaceGrafica();
    }

    // ==========================================
    //                 INTERFACE
    // ==========================================
    private void apresentarInterfaceGrafica() {
        GenericDialog dialog = new GenericDialog("Morfologia Binária (Cruz 3x3)");
        
        dialog.addMessage("Selecione a operação morfológica a ser aplicada.\n" +
                          "Nota: O algoritmo assume fundo preto (0) e objeto branco (255).");
        dialog.addRadioButtonGroup("Operações:", METODOS, 6, 1, METODOS[0]);
        
        dialog.showDialog();

        if (dialog.wasCanceled()) return;

        String operacaoSelecionada = dialog.getNextRadioButton();
        ImageProcessor imagemProcessador = imgAberta.getProcessor();

        direcionarOperacao(operacaoSelecionada, imagemProcessador);
    }

    private void direcionarOperacao(String operacao, ImageProcessor original) {
        if (operacao.equals(METODOS[0])) {
        	ImagePlus imgDilatacao = new ImagePlus("Dilatação", dilatacao(original));
        	imgDilatacao.show();
        } else if (operacao.equals(METODOS[1])) {
        	ImagePlus imgErosao =  new ImagePlus("Erosão", erosao(original));
            imgErosao.show();
        } else if (operacao.equals(METODOS[2])) {
            aplicarAbertura(original);
        } else if (operacao.equals(METODOS[3])) {
            aplicarFechamento(original);
        } else if (operacao.equals(METODOS[4])) {
            aplicarExtracaoBorda(original);
        } else {
            aplicarEsqueletizacao(original);
        }
    }

    // ==========================================
    //             OPERAÇÕES PRINCIPAIS
    // ==========================================
    private void aplicarAbertura(ImageProcessor original) {
        ImageProcessor imagemErodida = erosao(original);
        ImageProcessor imagemAberta = dilatacao(imagemErodida);
        ImagePlus imgAbertura = new ImagePlus("Abertura", imagemAberta);
        imgAbertura.show();
    }

    private void aplicarFechamento(ImageProcessor original) {
        ImageProcessor imagemDilatada = dilatacao(original);
        ImageProcessor imagemFechada = erosao(imagemDilatada);
        ImagePlus imgFechamento = new ImagePlus("Fechamento", imagemFechada);
        imgFechamento.show();
    }

    private void aplicarExtracaoBorda(ImageProcessor original) {
        ImageProcessor imagemErodida = erosao(original);
        ImageProcessor borda = diferenca(original, imagemErodida);
        ImagePlus imgBorda = new ImagePlus("Extração de Borda", borda);
        imgBorda.show();
    }

    private void aplicarEsqueletizacao(ImageProcessor original) {
        int largura = original.getWidth();
        int altura = original.getHeight();
        
        ImageProcessor esqueletoFinal = original.createProcessor(largura, altura);
        ImageProcessor imagemAtual = original.duplicate();
        
        int limiteIteracoes = 0; 
        
        while (!imagemVazia(imagemAtual) && limiteIteracoes < 1000) {
            ImageProcessor imagemErodida = erosao(imagemAtual);
            ImageProcessor imagemAberta = dilatacao(imagemErodida); 
            
            ImageProcessor camadaEsqueleto = diferenca(imagemAtual, imagemAberta);
            esqueletoFinal = uniao(esqueletoFinal, camadaEsqueleto);
            
            imagemAtual = imagemErodida;
            limiteIteracoes++;
        }
        
        ImagePlus imgEsqueleto = new ImagePlus("Esqueleto Final", esqueletoFinal);
        imgEsqueleto.show();
    }

    // ==========================================
    //             OPERAÇÕES BÁSICAS
    // ==========================================
    private ImageProcessor erosao(ImageProcessor original) {
        int largura = original.getWidth();
        int altura = original.getHeight();
        ImageProcessor resultado = original.createProcessor(largura, altura);
        
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                boolean todosBrancos = (obterPixel(original, x, y) == 255)   &&
                                       (obterPixel(original, x - 1, y) == 255) &&
                                       (obterPixel(original, x + 1, y) == 255) &&
                                       (obterPixel(original, x, y - 1) == 255) &&
                                       (obterPixel(original, x, y + 1) == 255);
                
                if (todosBrancos) resultado.putPixel(x, y, 255);
                else resultado.putPixel(x, y, 0);
            }
        }
        return resultado;
    }

    private ImageProcessor dilatacao(ImageProcessor original) {
        int largura = original.getWidth();
        int altura = original.getHeight();
        ImageProcessor resultado = original.createProcessor(largura, altura);
        
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                boolean algumBranco = (obterPixel(original, x, y) == 255)   ||
                                      (obterPixel(original, x - 1, y) == 255) ||
                                      (obterPixel(original, x + 1, y) == 255) ||
                                      (obterPixel(original, x, y - 1) == 255) ||
                                      (obterPixel(original, x, y + 1) == 255);
                
                if (algumBranco) resultado.putPixel(x, y, 255);
                else resultado.putPixel(x, y, 0);
            }
        }
        return resultado;
    }

    // ==========================================
    //             MÉTODOS AUXILIARES
    // ==========================================
    private ImageProcessor diferenca(ImageProcessor imgA, ImageProcessor imgB) {
        int largura = imgA.getWidth();
        int altura = imgA.getHeight();
        ImageProcessor resultado = imgA.createProcessor(largura, altura);
        
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                if (imgA.getPixel(x, y) == 255 && imgB.getPixel(x, y) == 0) {
                    resultado.putPixel(x, y, 255);
                } else {
                    resultado.putPixel(x, y, 0);
                }
            }
        }
        return resultado;
    }

    private ImageProcessor uniao(ImageProcessor imgA, ImageProcessor imgB) {
        int largura = imgA.getWidth();
        int altura = imgA.getHeight();
        ImageProcessor resultado = imgA.createProcessor(largura, altura);
        
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                if (imgA.getPixel(x, y) == 255 || imgB.getPixel(x, y) == 255) {
                    resultado.putPixel(x, y, 255);
                } else {
                    resultado.putPixel(x, y, 0);
                }
            }
        }
        return resultado;
    }

    private boolean imagemVazia(ImageProcessor img) {
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                if (img.getPixel(x, y) == 255) return false;
            }
        }
        return true;
    }

    private int obterPixel(ImageProcessor img, int x, int y) {
        if (x < 0 || x >= img.getWidth() || y < 0 || y >= img.getHeight()) {
            return 0; 
        }
        return img.getPixel(x, y);
    }
}
