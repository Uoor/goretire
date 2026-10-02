const path = require("node:path");
const CopyWebpackPlugin = require("copy-webpack-plugin");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const webpack = require("webpack");

const root = __dirname;
const basePath = process.env.PUBLIC_URL
  ? `/${process.env.PUBLIC_URL.split("/").filter(Boolean).join("/")}/`
  : "/";

module.exports = (_environment, arguments_) => ({
  mode: arguments_.mode || "development",
  entry: path.join(root, "src/main.tsx"),
  output: {
    path: path.join(root, "dist"),
    filename: "assets/[name].[contenthash].js",
    publicPath: basePath,
    clean: true
  },
  resolve: { extensions: [".tsx", ".ts", ".js"] },
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        exclude: /node_modules/,
        use: { loader: "ts-loader", options: { transpileOnly: true } }
      },
      {
        test: /\.scss$/,
        use: [MiniCssExtractPlugin.loader, "css-loader", "sass-loader"]
      }
    ]
  },
  plugins: [
    new MiniCssExtractPlugin({ filename: "assets/[name].[contenthash].css" }),
    new webpack.DefinePlugin({ __APP_BASE_PATH__: JSON.stringify(basePath) }),
    new HtmlWebpackPlugin({
      template: path.join(root, "index.html"),
      filename: "index.html",
      title: "一起提前退休｜互联网人互助社群",
      chunks: ["main"]
    }),
    new HtmlWebpackPlugin({
      template: path.join(root, "index.html"),
      filename: "404.html",
      title: "一起提前退休｜互联网人互助社群",
      chunks: ["main"]
    }),
    new CopyWebpackPlugin({
      patterns: [
        { from: path.join(root, ".nojekyll"), to: ".nojekyll", toType: "file" },
        { from: path.join(root, "wxpic.png"), to: "wxpic.png" },
        { from: path.join(root, "assets", "content-data.js"), to: "assets/content-data.js" },
        { from: path.join(root, "assets"), to: "assets", globOptions: { ignore: ["**/*.js", "**/*.scss", "**/*.css", "**/*.md", "**/Bold_poster_style*"] } }
      ]
    })
  ],
  devServer: {
    port: 8774,
    static: { directory: path.join(root, "dist") },
    historyApiFallback: true,
    hot: true,
    open: false
  }
});