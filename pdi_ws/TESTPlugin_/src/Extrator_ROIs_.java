import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import java.io.File;
import ij.plugin.ChannelSplitter;

public class Extrator_ROIs_ implements PlugIn {

    // ==========================================
    // 1. PONTO DE ENTRADA E SELEÇÃO DE DIRETÓRIOS
    // ==========================================
    @Override
    public void run(String arg) {
        // Solicita o diretório de entrada (onde estão as imagens originais)
        DirectoryChooser dcEntrada = new DirectoryChooser("Selecione o diretório de ENTRADA (Imagens Originais)");
        String dirEntrada = dcEntrada.getDirectory();
        if (dirEntrada == null) return; // Usuário cancelou

        // Solicita o diretório de saída (onde as ROIs serão salvas)
        DirectoryChooser dcSaida = new DirectoryChooser("Selecione o diretório de SAÍDA (ROIs extraídas)");
        String dirSaida = dcSaida.getDirectory();
        if (dirSaida == null) return; // Usuário cancelou

        File pastaEntrada = new File(dirEntrada);
        File[] arquivos = pastaEntrada.listFiles();

        if (arquivos == null || arquivos.length == 0) {
            IJ.error("Erro", "Nenhum arquivo encontrado no diretório de entrada.");
            return;
        }

        // Instancia o gerenciador de ROIs nativo do ImageJ
        RoiManager roiManager = RoiManager.getInstance();
        if (roiManager == null) {
            roiManager = new RoiManager();
        }

        // ==========================================
        // 2. FLUXO DE CONTROLE (Varredura de Arquivos)
        // ==========================================
        int totalImagensProcessadas = 0;
        int totalRoisExtraidas = 0;

        for (File arquivo : arquivos) {
            // Verifica se é um arquivo (ignora subpastas) e se tem extensão de imagem
            if (arquivo.isFile() && arquivoEhImagem(arquivo.getName())) {
                int roisNaImagem = extrairESalvarROIs(arquivo, dirSaida, roiManager);
                totalRoisExtraidas += roisNaImagem;
                totalImagensProcessadas++;
            }
        }

        if (roiManager != null) {
            roiManager.close();
        }
        
        // Feedback final para o usuário
        IJ.showMessage("Processamento Concluído", 
                       "Imagens processadas: " + totalImagensProcessadas + "\n" +
                       "ROIs extraídas e salvas: " + totalRoisExtraidas);
    }

    // ==========================================
    // 3. LÓGICA CENTRAL (Processamento por Imagem)
    // ==========================================
    private int extrairESalvarROIs(File arquivo, String dirSaida, RoiManager roiManager) {
        // 1. Obter a imagem original (colorida/tons de cinza original)
        ImagePlus imgOriginal = IJ.openImage(arquivo.getAbsolutePath());
        if (imgOriginal == null) return 0;

        // Duplica a imagem para não estragar a original durante as modificações
        ImagePlus imgProcessamento = imgOriginal.duplicate();

        // 2. Separar os canais RGB e usar apenas o Canal Azul
        ImagePlus[] canais = ChannelSplitter.split(imgProcessamento);

        imgProcessamento.close(); // Fecha a imagem RGB temporária da memória
        imgProcessamento = canais[2]; // O índice 2 é o Canal Azul (0=Red, 1=Green, 2=Blue)

        // Limpa os canais que não vamos usar para economizar RAM
        canais[0].close(); 
        canais[1].close();

        // 3. Realizar o Threshold na imagem (Binarização)
        // Como você usará cones em fundo branco, "background=Light" avisa o ImageJ que o fundo é claro.
        IJ.run(imgProcessamento, "Convert to Mask", "method=Default background=Light black");
        
        // Operação extra para garantir que o interior dos cones fique sólido
        IJ.run(imgProcessamento, "Fill Holes", "");
        
        // Corta objetos que estão "colados" um no outro
        IJ.run(imgProcessamento, "Watershed", "");

        // Limpa o RoiManager para não misturar as ROIs desta imagem com as da imagem anterior
        roiManager.reset();

        // 4. Executar o Analyze Particles
        // O parâmetro "size=100-Infinity" evita que pequenos ruídos ou sujeiras sejam classificados como cones.
        // O parâmetro "add" é o que joga as regiões encontradas no RoiManager.
        IJ.run(imgProcessamento, "Analyze Particles...", "size=100-Infinity add");

        // 5. Extração e Salvamento
        Roi[] roisDetectadas = roiManager.getRoisAsArray();
        String nomeBase = obterNomeSemExtensao(arquivo.getName());

        for (int i = 0; i < roisDetectadas.length; i++) {
            // Seta o ROI detectado na máscara binária de volta na imagem ORIGINAL (Colorida)
            imgOriginal.setRoi(roisDetectadas[i]);
            
            // Recorta (Crop) a região delimitada
            ImageProcessor procRecortado = imgOriginal.getProcessor().crop();
            
            // Transforma o recorte em um novo ImagePlus independente
            ImagePlus imgRecortada = new ImagePlus("ROI_" + i, procRecortado);
            
            // Grava a imagem no diretório de destino
            // Exemplo de saída: "meu_cone_roi_1.png"
            String caminhoSaida = dirSaida + File.separator + nomeBase + "_roi_" + (i + 1) + ".png";
            IJ.saveAs(imgRecortada, "PNG", caminhoSaida);
        }

        // Fecha as imagens abertas na memória para não sobrecarregar o computador
        imgOriginal.close();
        imgProcessamento.close();

        return roisDetectadas.length;
    }

    // ==========================================
    // 4. MÉTODOS AUXILIARES
    // ==========================================
    private boolean arquivoEhImagem(String nome) {
        String nomeMinusculo = nome.toLowerCase();
        return nomeMinusculo.endsWith(".jpg") || 
               nomeMinusculo.endsWith(".jpeg") || 
               nomeMinusculo.endsWith(".png") || 
               nomeMinusculo.endsWith(".tif") || 
               nomeMinusculo.endsWith(".tiff");
    }

    private String obterNomeSemExtensao(String nomeArquivo) {
        int indicePonto = nomeArquivo.lastIndexOf('.');
        if (indicePonto > 0) {
            return nomeArquivo.substring(0, indicePonto);
        }
        return nomeArquivo;
    }
}